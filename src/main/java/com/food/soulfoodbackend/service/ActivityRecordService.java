package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.domain.entity.SfActivityRecord;
import com.food.soulfoodbackend.dto.record.RecordItemDto;
import com.food.soulfoodbackend.dto.record.RecordStatDto;
import com.food.soulfoodbackend.dto.record.RecordsOverviewResponse;
import com.food.soulfoodbackend.mapper.SfActivityRecordMapper;
import com.food.soulfoodbackend.mapper.SfVoteMapper;
import com.food.soulfoodbackend.domain.entity.SfVote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final SfActivityRecordMapper activityRecordMapper;
    private final SfVoteMapper voteMapper;

    public void recordVote(Long userId, String roomCode, String optionTitle) {
        save(userId, "vote", "参与投票：" + optionTitle, "房间 " + roomCode, null);
    }

    public void recordRoomResult(Long userId, String roomCode, String winnerTitle) {
        save(userId, "vote", "投票结果：" + winnerTitle, "房间 " + roomCode, null);
    }

    public void recordWantRestaurant(Long userId, String restaurantName) {
        save(userId, "eat", "标记想去", restaurantName, null);
    }

    private void save(Long userId, String type, String title, String summary, Long refId) {
        SfActivityRecord record = new SfActivityRecord();
        record.setUserId(userId);
        record.setRecordType(type);
        record.setTitle(title);
        record.setSummary(summary);
        record.setRefId(refId);
        record.setOccurredAt(OffsetDateTime.now());
        record.setCreatedAt(OffsetDateTime.now());
        activityRecordMapper.insert(record);
    }

    public RecordsOverviewResponse overview(Long userId) {
        long voteTimes = voteMapper.selectCount(new LambdaQueryWrapper<SfVote>().eq(SfVote::getUserId, userId));
        long eatTimes = activityRecordMapper.selectCount(new LambdaQueryWrapper<SfActivityRecord>()
                .eq(SfActivityRecord::getUserId, userId)
                .eq(SfActivityRecord::getRecordType, "eat"));
        long favoriteTimes = activityRecordMapper.selectCount(new LambdaQueryWrapper<SfActivityRecord>()
                .eq(SfActivityRecord::getUserId, userId)
                .eq(SfActivityRecord::getRecordType, "favorite"));

        List<RecordStatDto> stats = List.of(
                new RecordStatDto("vote", "投票", (int) voteTimes),
                new RecordStatDto("friends", "和朋友", (int) voteTimes),
                new RecordStatDto("eat", "吃过", (int) eatTimes),
                new RecordStatDto("favorite", "收藏", (int) favoriteTimes)
        );

        List<SfActivityRecord> recentRows = activityRecordMapper.selectList(
                new LambdaQueryWrapper<SfActivityRecord>()
                        .eq(SfActivityRecord::getUserId, userId)
                        .orderByDesc(SfActivityRecord::getOccurredAt)
                        .last("LIMIT 20"));

        List<RecordItemDto> recent = recentRows.stream()
                .map(r -> new RecordItemDto(
                        r.getId(),
                        r.getRecordType(),
                        r.getTitle(),
                        r.getSummary(),
                        r.getOccurredAt() == null ? "" : DISPLAY_TIME.format(r.getOccurredAt())))
                .toList();

        return new RecordsOverviewResponse(stats, recent);
    }
}
