import { ConfigProvider, Layout, Menu, Card, Space, Typography, Tag, Button } from 'antd'
import {
  FileTextOutlined,
  MessageOutlined,
  SettingOutlined,
  DashboardOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import zhCN from 'antd/locale/zh_CN'
import { t } from './locales'
import './App.css'

const { Header, Content, Footer } = Layout
const { Title, Paragraph, Text } = Typography

function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <Layout style={{ minHeight: '100vh' }}>
        <Header style={{ display: 'flex', alignItems: 'center', background: '#001529' }}>
          <div style={{ color: 'white', fontSize: '20px', fontWeight: 'bold' }}>
            智能运维问答助手
          </div>
          <Menu
            theme="dark"
            mode="horizontal"
            defaultSelectedKeys={['1']}
            style={{ flex: 1, minWidth: 0, marginLeft: '50px' }}
            items={[
              { key: '1', icon: <DashboardOutlined />, label: t('menu.home') },
              { key: '2', icon: <FileTextOutlined />, label: t('menu.documents') },
              { key: '3', icon: <MessageOutlined />, label: t('menu.query') },
              { key: '4', icon: <SettingOutlined />, label: t('menu.config') },
            ]}
          />
        </Header>
        
        <Content style={{ padding: '50px' }}>
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Card>
              <Title level={2}>欢迎使用智能运维问答助手</Title>
              <Paragraph>
                这是一个基于 RAG（检索增强生成）技术的企业级智能问答系统，采用微服务架构设计。
              </Paragraph>
              <Paragraph>
                <Text strong>核心功能：</Text>
              </Paragraph>
              <Space direction="vertical">
                <Text><CheckCircleOutlined style={{ color: '#52c41a' }} /> 文档智能上传与处理</Text>
                <Text><CheckCircleOutlined style={{ color: '#52c41a' }} /> 多轮对话问答</Text>
                <Text><CheckCircleOutlined style={{ color: '#52c41a' }} /> 知识库管理</Text>
                <Text><CheckCircleOutlined style={{ color: '#52c41a' }} /> 系统配置与监控</Text>
              </Space>
            </Card>

            <Card title="系统状态">
              <Space size="large">
                <Tag color="success">服务运行正常</Tag>
                <Tag color="processing">微服务架构</Tag>
                <Tag color="default">中文界面支持</Tag>
                <Tag color="blue">UTF-8 编码</Tag>
              </Space>
            </Card>

            <Card title="快速开始">
              <Space>
                <Button type="primary" icon={<FileTextOutlined />}>
                  {t('document.upload')}
                </Button>
                <Button icon={<MessageOutlined />}>
                  {t('query.newSession')}
                </Button>
                <Button icon={<SettingOutlined />}>
                  {t('config.title')}
                </Button>
              </Space>
            </Card>

            <Card title="语言支持测试">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Paragraph>
                  <Text strong>中文显示测试：</Text>
                </Paragraph>
                <Paragraph>
                  常用词汇：{t('common.confirm')}、{t('common.cancel')}、{t('common.save')}、
                  {t('common.delete')}、{t('common.search')}
                </Paragraph>
                <Paragraph>
                  文档管理：{t('document.title')}、{t('document.upload')}、{t('document.filename')}、
                  {t('document.processing')}、{t('document.completed')}
                </Paragraph>
                <Paragraph>
                  智能问答：{t('query.title')}、{t('query.inputPlaceholder')}、{t('query.send')}、
                  {t('query.sources')}、{t('query.similarity')}
                </Paragraph>
                <Paragraph>
                  <Text type="success">✓ 中文字符显示正常</Text>
                </Paragraph>
                <Paragraph>
                  <Text type="success">✓ UTF-8 编码支持</Text>
                </Paragraph>
                <Paragraph>
                  <Text type="success">✓ Ant Design 中文语言包已配置</Text>
                </Paragraph>
                <Paragraph>
                  <Text type="success">✓ Day.js 中文语言包已配置</Text>
                </Paragraph>
              </Space>
            </Card>
          </Space>
        </Content>
        
        <Footer style={{ textAlign: 'center' }}>
          智能运维问答助手 ©2024 - 基于微服务架构的 RAG 系统
        </Footer>
      </Layout>
    </ConfigProvider>
  )
}

export default App
