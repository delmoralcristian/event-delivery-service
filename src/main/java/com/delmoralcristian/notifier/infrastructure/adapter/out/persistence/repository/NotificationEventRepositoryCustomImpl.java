package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.repository;

import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationEventRepositoryCustomImpl implements NotificationEventRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<NotificationEventEntity> ROW_MAPPER = (rs, rowNum) ->
        NotificationEventEntity.builder()
            .id(rs.getLong("id"))
            .eventId(rs.getString("event_id"))
            .eventType(rs.getString("event_type"))
            .content(rs.getString("content"))
            .deliveryDate(rs.getObject("delivery_date", LocalDateTime.class))
            .deliveryStatus(rs.getString("delivery_status"))
            .clientId(rs.getString("client_id"))
            .webhookUrl(rs.getString("webhook_url"))
            .build();

    @Override
    public Page<NotificationEventEntity> findByFilters(
        String clientId, String status, LocalDateTime from, LocalDateTime to, Pageable pageable
    ) {
        var where = new StringBuilder(" WHERE client_id = :clientId");
        var params = new MapSqlParameterSource("clientId", clientId);

        if (status != null) {
            where.append(" AND delivery_status = :status");
            params.addValue("status", status);
        }
        if (from != null) {
            where.append(" AND delivery_date >= :from");
            params.addValue("from", from);
        }
        if (to != null) {
            where.append(" AND delivery_date <= :to");
            params.addValue("to", to);
        }

        var sql = "SELECT *, COUNT(*) OVER() AS total_count FROM notification_event"
            + where + " ORDER BY delivery_date DESC LIMIT :limit OFFSET :offset";
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        long[] total = {0};
        List<NotificationEventEntity> rows = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            if (rowNum == 0) total[0] = rs.getLong("total_count");
            return ROW_MAPPER.mapRow(rs, rowNum);
        });

        return new PageImpl<>(rows, pageable, total[0]);
    }
}
