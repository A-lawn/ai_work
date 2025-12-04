FROM docker.elastic.co/beats/filebeat:8.11.0

USER root

# 复制配置文件
COPY filebeat.yml /usr/share/filebeat/filebeat.yml

# 设置权限
RUN chown root:filebeat /usr/share/filebeat/filebeat.yml \
    && chmod 644 /usr/share/filebeat/filebeat.yml

# 创建日志目录
RUN mkdir -p /var/log/filebeat \
    && chown filebeat:filebeat /var/log/filebeat

USER filebeat

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD filebeat test config

ENTRYPOINT ["/usr/share/filebeat/filebeat"]
CMD ["-e", "-c", "/usr/share/filebeat/filebeat.yml"]
