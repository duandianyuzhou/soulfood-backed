package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfRoom;
import com.food.soulfoodbackend.domain.entity.SfRoomOption;
import com.food.soulfoodbackend.domain.entity.SfVote;
import com.food.soulfoodbackend.domain.entity.SfUser;
import com.food.soulfoodbackend.dto.room.AddOptionRequest;
import com.food.soulfoodbackend.dto.room.CastVoteRequest;
import com.food.soulfoodbackend.dto.room.CreateRoomRequest;
import com.food.soulfoodbackend.dto.room.CreateRoomResponse;
import com.food.soulfoodbackend.dto.room.FriendRoomDto;
import com.food.soulfoodbackend.dto.room.RoomDetailResponse;
import com.food.soulfoodbackend.dto.room.RoomOptionDto;
import com.food.soulfoodbackend.dto.room.RoomResultResponse;
import com.food.soulfoodbackend.mapper.SfRoomMapper;
import com.food.soulfoodbackend.mapper.SfRoomOptionMapper;
import com.food.soulfoodbackend.mapper.SfUserMapper;
import com.food.soulfoodbackend.mapper.SfVoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RoomService {

    /** 房间创建后超过该时长自动结束投票 */
    public static final int AUTO_CLOSE_MINUTES = 30;

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final SfRoomMapper roomMapper;
    private final SfRoomOptionMapper optionMapper;
    private final SfVoteMapper voteMapper;
    private final SfUserMapper userMapper;
    private final FriendService friendService;
    private final ActivityRecordService activityRecordService;

    @Transactional
    public CreateRoomResponse createRoom(Long ownerId, CreateRoomRequest request) {
        int durationMin = normalizeDurationMin(request.getDurationMin());
        SfRoom room = new SfRoom();
        room.setCode(generateUniqueCode());
        room.setTopic(request.getTopic().trim());
        room.setMaxPeople(request.getMaxPeople());
        room.setDurationMin(durationMin);
        room.setStatus("voting");
        room.setOwnerId(ownerId);
        room.setCreatedAt(OffsetDateTime.now());
        room.setUpdatedAt(OffsetDateTime.now());
        room.setDeleted(false);
        roomMapper.insert(room);

        List<String> titles = request.getInitialOptions();
        if (titles == null || titles.isEmpty()) {
            titles = List.of("火锅", "烤肉");
        }
        int order = 0;
        for (String title : titles) {
            if (title == null || title.isBlank()) {
                continue;
            }
            insertOption(room.getId(), title.trim(), "manual", order++);
        }
        return new CreateRoomResponse(room.getCode(), room.getTopic());
    }

    public RoomDetailResponse getRoomDetail(String code, Long currentUserId) {
        SfRoom room = requireActiveOrClosedRoom(code);
        List<SfRoomOption> options = listOptions(room.getId());
        int totalVotes = options.stream().mapToInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()).sum();
        Long myVoteOptionId = null;
        if (currentUserId != null) {
            SfVote vote = voteMapper.selectOne(new LambdaQueryWrapper<SfVote>()
                    .eq(SfVote::getRoomId, room.getId())
                    .eq(SfVote::getUserId, currentUserId));
            if (vote != null) {
                myVoteOptionId = vote.getOptionId();
            }
        }
        int participants = Math.toIntExact(voteMapper.selectCount(new LambdaQueryWrapper<SfVote>()
                .eq(SfVote::getRoomId, room.getId())));

        List<RoomOptionDto> optionDtos = new ArrayList<>();
        for (SfRoomOption option : options) {
            int count = option.getVoteCount() == null ? 0 : option.getVoteCount();
            int percent = totalVotes == 0 ? 0 : (int) Math.round(count * 100.0 / totalVotes);
            optionDtos.add(new RoomOptionDto(option.getId(), option.getTitle(), count, percent, option.getSource()));
        }

        OffsetDateTime expiresAt = expiresAt(room);
        long remainingSeconds = remainingSeconds(room);

        return new RoomDetailResponse(
                room.getCode(),
                room.getTopic(),
                room.getStatus(),
                room.getMaxPeople(),
                room.getDurationMin(),
                participants,
                myVoteOptionId,
                expiresAt == null ? null : ISO_TIME.format(expiresAt),
                remainingSeconds,
                optionDtos
        );
    }

    public RoomDetailResponse joinRoom(String code, Long userId) {
        requireActiveOrClosedRoom(code);
        return getRoomDetail(code, userId);
    }

    public List<FriendRoomDto> listFriendRooms(Long userId) {
        List<Long> friendIds = friendService.listFriendUserIds(userId);
        if (friendIds.isEmpty()) {
            return List.of();
        }

        List<SfRoom> rooms = roomMapper.selectList(new LambdaQueryWrapper<SfRoom>()
                .in(SfRoom::getOwnerId, friendIds)
                .in(SfRoom::getStatus, List.of("open", "voting"))
                .orderByDesc(SfRoom::getCreatedAt)
                .last("LIMIT 20"));

        List<FriendRoomDto> result = new ArrayList<>();
        for (SfRoom room : rooms) {
            closeIfExpired(room);
            if (!"voting".equals(room.getStatus()) && !"open".equals(room.getStatus())) {
                continue;
            }
            SfUser owner = userMapper.selectById(room.getOwnerId());
            int participants = Math.toIntExact(voteMapper.selectCount(new LambdaQueryWrapper<SfVote>()
                    .eq(SfVote::getRoomId, room.getId())));
            result.add(new FriendRoomDto(
                    room.getCode(),
                    room.getTopic(),
                    room.getStatus(),
                    room.getOwnerId(),
                    owner != null ? owner.getNickname() : "好友",
                    participants,
                    room.getCreatedAt() == null ? "" : DISPLAY_TIME.format(room.getCreatedAt())
            ));
        }
        return result;
    }

    @Transactional
    public RoomOptionDto addOption(String code, AddOptionRequest request) {
        SfRoom room = requireOpenRoom(code);
        int order = listOptions(room.getId()).size();
        SfRoomOption option = insertOption(room.getId(), request.getTitle().trim(),
                request.getSource() != null ? request.getSource() : "manual", order);
        return new RoomOptionDto(option.getId(), option.getTitle(), 0, 0, option.getSource());
    }

    @Transactional
    public RoomDetailResponse castVote(String code, Long userId, CastVoteRequest request) {
        SfRoom room = requireOpenRoom(code);
        SfRoomOption target = optionMapper.selectById(request.getOptionId());
        if (target == null || !target.getRoomId().equals(room.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "选项不存在");
        }

        SfVote existing = voteMapper.selectOne(new LambdaQueryWrapper<SfVote>()
                .eq(SfVote::getRoomId, room.getId())
                .eq(SfVote::getUserId, userId));

        if (existing != null) {
            if (existing.getOptionId().equals(request.getOptionId())) {
                return getRoomDetail(code, userId);
            }
            SfRoomOption oldOption = optionMapper.selectById(existing.getOptionId());
            if (oldOption != null && oldOption.getVoteCount() != null && oldOption.getVoteCount() > 0) {
                oldOption.setVoteCount(oldOption.getVoteCount() - 1);
                oldOption.setUpdatedAt(OffsetDateTime.now());
                optionMapper.updateById(oldOption);
            }
            existing.setOptionId(request.getOptionId());
            voteMapper.updateById(existing);
        } else {
            SfVote vote = new SfVote();
            vote.setRoomId(room.getId());
            vote.setOptionId(request.getOptionId());
            vote.setUserId(userId);
            vote.setCreatedAt(OffsetDateTime.now());
            voteMapper.insert(vote);
        }

        target.setVoteCount((target.getVoteCount() == null ? 0 : target.getVoteCount()) + 1);
        target.setUpdatedAt(OffsetDateTime.now());
        optionMapper.updateById(target);

        activityRecordService.recordVote(userId, room.getCode(), target.getTitle());
        return getRoomDetail(code, userId);
    }

    @Transactional
    public RoomResultResponse getResult(String code, Long userId) {
        SfRoom room = requireActiveOrClosedRoom(code);
        if ("closed".equals(room.getStatus())) {
            return buildResultFromRoom(room);
        }
        return closeRoomWithWinner(room, userId);
    }

    /** 每分钟扫描并自动结束超时房间 */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void closeExpiredRoomsJob() {
        List<SfRoom> rooms = roomMapper.selectList(new LambdaQueryWrapper<SfRoom>()
                .in(SfRoom::getStatus, List.of("open", "voting")));
        for (SfRoom room : rooms) {
            if (isExpired(room)) {
                closeRoomWithWinner(room, room.getOwnerId());
            }
        }
    }

    private SfRoom requireActiveOrClosedRoom(String code) {
        SfRoom room = findRoom(code);
        closeIfExpired(room);
        return roomMapper.selectById(room.getId());
    }

    private SfRoom requireOpenRoom(String code) {
        SfRoom room = requireActiveOrClosedRoom(code);
        if ("closed".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "投票已结束（超过30分钟或已手动结束）");
        }
        return room;
    }

    private SfRoom findRoom(String code) {
        SfRoom room = roomMapper.selectOne(new LambdaQueryWrapper<SfRoom>().eq(SfRoom::getCode, code));
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "房间不存在");
        }
        return room;
    }

    private void closeIfExpired(SfRoom room) {
        if ("closed".equals(room.getStatus()) || !isExpired(room)) {
            return;
        }
        closeRoomWithWinner(room, room.getOwnerId());
    }

    private boolean isExpired(SfRoom room) {
        OffsetDateTime expiresAt = expiresAt(room);
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    private OffsetDateTime expiresAt(SfRoom room) {
        if (room.getCreatedAt() == null) {
            return null;
        }
        int minutes = room.getDurationMin() == null ? AUTO_CLOSE_MINUTES : Math.min(room.getDurationMin(), AUTO_CLOSE_MINUTES);
        return room.getCreatedAt().plusMinutes(minutes);
    }

    private long remainingSeconds(SfRoom room) {
        if ("closed".equals(room.getStatus())) {
            return 0L;
        }
        OffsetDateTime expiresAt = expiresAt(room);
        if (expiresAt == null) {
            return 0L;
        }
        long seconds = java.time.Duration.between(OffsetDateTime.now(), expiresAt).getSeconds();
        return Math.max(0L, seconds);
    }

    private int normalizeDurationMin(Integer durationMin) {
        if (durationMin == null || durationMin <= 0) {
            return AUTO_CLOSE_MINUTES;
        }
        return Math.min(durationMin, AUTO_CLOSE_MINUTES);
    }

    @Transactional
    protected RoomResultResponse closeRoomWithWinner(SfRoom room, Long userId) {
        if ("closed".equals(room.getStatus())) {
            return buildResultFromRoom(room);
        }
        List<SfRoomOption> options = listOptions(room.getId());
        if (options.isEmpty()) {
            room.setStatus("closed");
            room.setClosedAt(OffsetDateTime.now());
            room.setUpdatedAt(OffsetDateTime.now());
            roomMapper.updateById(room);
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无投票选项");
        }
        SfRoomOption winner = options.stream()
                .max(Comparator.comparingInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()))
                .orElseThrow();

        room.setWinnerOptionId(winner.getId());
        room.setStatus("closed");
        room.setClosedAt(OffsetDateTime.now());
        room.setUpdatedAt(OffsetDateTime.now());
        roomMapper.updateById(room);

        Long recordUserId = userId != null ? userId : room.getOwnerId();
        if (recordUserId != null) {
            activityRecordService.recordRoomResult(recordUserId, room.getCode(), winner.getTitle());
        }

        int total = options.stream().mapToInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()).sum();
        int percent = total == 0 ? 0 : (int) Math.round(winner.getVoteCount() * 100.0 / total);
        return new RoomResultResponse(winner.getId(), winner.getTitle(), winner.getVoteCount(), percent);
    }

    private RoomResultResponse buildResultFromRoom(SfRoom room) {
        if (room.getWinnerOptionId() != null) {
            SfRoomOption winner = optionMapper.selectById(room.getWinnerOptionId());
            if (winner != null) {
                List<SfRoomOption> options = listOptions(room.getId());
                int total = options.stream().mapToInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()).sum();
                int percent = total == 0 ? 0 : (int) Math.round(winner.getVoteCount() * 100.0 / total);
                return new RoomResultResponse(winner.getId(), winner.getTitle(), winner.getVoteCount(), percent);
            }
        }
        List<SfRoomOption> options = listOptions(room.getId());
        if (options.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无投票结果");
        }
        SfRoomOption winner = options.stream()
                .max(Comparator.comparingInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()))
                .orElseThrow();
        int total = options.stream().mapToInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()).sum();
        int percent = total == 0 ? 0 : (int) Math.round(winner.getVoteCount() * 100.0 / total);
        return new RoomResultResponse(winner.getId(), winner.getTitle(), winner.getVoteCount(), percent);
    }

    private List<SfRoomOption> listOptions(Long roomId) {
        return optionMapper.selectList(new LambdaQueryWrapper<SfRoomOption>()
                .eq(SfRoomOption::getRoomId, roomId)
                .orderByAsc(SfRoomOption::getSortOrder)
                .orderByAsc(SfRoomOption::getId));
    }

    private SfRoomOption insertOption(Long roomId, String title, String source, int sortOrder) {
        SfRoomOption option = new SfRoomOption();
        option.setRoomId(roomId);
        option.setTitle(title);
        option.setVoteCount(0);
        option.setSource(source);
        option.setSortOrder(sortOrder);
        option.setCreatedAt(OffsetDateTime.now());
        option.setUpdatedAt(OffsetDateTime.now());
        option.setDeleted(false);
        optionMapper.insert(option);
        return option;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            Long count = roomMapper.selectCount(new LambdaQueryWrapper<SfRoom>().eq(SfRoom::getCode, code));
            if (count == 0) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL, "生成房间号失败");
    }
}
