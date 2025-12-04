#!/bin/bash
# 生成自签名 SSL 证书用于开发和测试
# 生产环境请使用 Let's Encrypt 或其他 CA 签发的证书

set -e

CERT_DIR="./infrastructure/nginx/ssl"
DAYS=365

echo "生成自签名 SSL 证书..."

# 创建 SSL 目录
mkdir -p "$CERT_DIR"

# 生成私钥
openssl genrsa -out "$CERT_DIR/key.pem" 2048

# 生成证书签名请求（CSR）
openssl req -new -key "$CERT_DIR/key.pem" -out "$CERT_DIR/cert.csr" \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=RAG System/OU=IT/CN=localhost"

# 生成自签名证书
openssl x509 -req -days $DAYS -in "$CERT_DIR/cert.csr" \
    -signkey "$CERT_DIR/key.pem" -out "$CERT_DIR/cert.pem"

# 删除 CSR 文件
rm "$CERT_DIR/cert.csr"

echo "SSL 证书生成完成！"
echo "证书位置: $CERT_DIR/cert.pem"
echo "私钥位置: $CERT_DIR/key.pem"
echo "有效期: $DAYS 天"
echo ""
echo "注意: 这是自签名证书，仅用于开发和测试环境。"
echo "生产环境请使用 Let's Encrypt 或其他 CA 签发的证书。"
