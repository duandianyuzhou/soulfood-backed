package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfRoom;
import com.food.soulfoodbackend.domain.entity.SfRoomOption;
import com.food.soulfoodbackend.domain.entity.SfVote;
import com.food.soulfoodbackend.dto.room.AddOptionRequest;
import com.food.soulfoodbackend.dto.room.CastVoteRequest;
import com.food.soulfoodbackend.dto.room.CreateRoomRequest;
import com.food.soulfoodbackend.dto.room.CreateRoomResponse;
import com.food.soulfoodbackend.dto.room.RoomDetailResponse;
import com.food.soulfoodbackend.dto.room.RoomOptionDto;
import com.food.soulfoodbackend.dto.room.RoomResultResponse;
import com.food.soulfoodbackend.mapper.SfRoomMapper;
import com.food.soulfoodbackend.mapper.SfRoomOptionMapper;
import com.food.soulfoodbackend.mapper.SfVoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final SfRoomMapper roomMapper;
    private final SfRoomOptionMapper optionMapper;
    private final SfVoteMapper voteMapper;
    private final ActivityRecordService activityRecordService;

    @Transactional
    public CreateRoomResponse createRoom(Long ownerId, CreateRoomRequest request) {
        SfRoom room = new SfRoom();
        room.setCode(generateUniqueCode());
        room.setTopic(request.getTopic().trim());
        room.setMaxPeople(request.getMaxPeople());
        room.setDurationMin(request.getDurationMin());
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
        SfRoom room = requireRoom(code);
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

        return new RoomDetailResponse(
                room.getCode(),
                room.getTopic(),
                room.getStatus(),
                room.getMaxPeople(),
                room.getDurationMin(),
                participants,
                myVoteOptionId,
                optionDtos
        );
    }

    public RoomDetailResponse joinRoom(String code, Long userId) {
        requireRoom(code);
        return getRoomDetail(code, userId);
    }

    @Transactional
    public RoomOptionDto addOption(String code, AddOptionRequest request) {
        SfRoom room = requireRoom(code);
        int order = listOptions(room.getId()).size();
        SfRoomOption option = insertOption(room.getId(), request.getTitle().trim(),
                request.getSource() != null ? request.getSource() : "manual", order);
        return new RoomOptionDto(option.getId(), option.getTitle(), 0, 0, option.getSource());
    }

    @Transactional
    public RoomDetailResponse castVote(String code, Long userId, CastVoteRequest request) {
        SfRoom room = requireRoom(code);
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

    public RoomResultResponse getResult(String code, Long userId) {
        SfRoom room = requireRoom(code);
        List<SfRoomOption> options = listOptions(room.getId());
        if (options.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无投票选项");
        }
        SfRoomOption winner = options.stream()
                .max(Comparator.comparingInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()))
                .orElseThrow();
        int total = options.stream().mapToInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()).sum();
        int percent = total == 0 ? 0 : (int) Math.round(winner.getVoteCount() * 100.0 / total);

        room.setWinnerOptionId(winner.getId());
        room.setStatus("closed");
        room.setClosedAt(OffsetDateTime.now());
        room.setUpdatedAt(OffsetDateTime.now());
        roomMapper.updateById(room);

        if (userId != null) {
            activityRecordService.recordRoomResult(userId, room.getCode(), winner.getTitle());
        }
        return new RoomResultResponse(winner.getId(), winner.getTitle(), winner.getVoteCount(), percent);
    }

    private SfRoom requireRoom(String code) {
        SfRoom room = roomMapper.selectOne(new LambdaQueryWrapper<SfRoom>().eq(SfRoom::getCode, code));
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "房间不存在");
        }
        return room;
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
