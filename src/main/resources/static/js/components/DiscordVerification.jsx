import React, { useState } from 'react';
import axios from 'axios';
import { Button, Card, Typography, Space, Alert, Spin, Input, message } from 'antd';
import { DiscordOutlined, LinkOutlined, CopyOutlined } from '@ant-design/icons';

const { Text, Title } = Typography;

const DiscordVerification = () => {
  const [verificationCode, setVerificationCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const generateCode = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.get('/api/discord/generate-code');
      setVerificationCode(response.data.code);
      message.success('Код верификации успешно сгенерирован!');
    } catch (err) {
      setError('Не удалось сгенерировать код верификации. Пожалуйста, попробуйте позже.');
      message.error('Ошибка при генерации кода');
    } finally {
      setLoading(false);
    }
  };
  
  const copyToClipboard = () => {
    navigator.clipboard.writeText(verificationCode)
      .then(() => message.success('Код скопирован!'))
      .catch(() => message.error('Не удалось скопировать код'));
  };
  
  return (
    <Card title={<><DiscordOutlined /> Привязка Discord-аккаунта</>} style={{ width: '100%', maxWidth: 500, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {error && (
          <Alert message={error} type="error" showIcon />
        )}
        
        <Title level={4}>Как привязать Discord к вашему аккаунту:</Title>
        <Text>
          1. Нажмите кнопку "Сгенерировать код верификации"
        </Text>
        <Text>
          2. Добавьте нашего бота в Discord: <a href="https://discord.gg/our-bot" target="_blank" rel="noopener noreferrer">Пригласить бота</a>
        </Text>
        <Text>
          3. Напишите нашему боту в Discord команду <Text strong copyable>!verif</Text>
        </Text>
        <Text>
          4. Когда бот запросит код верификации, отправьте ему код, полученный ниже
        </Text>
        
        <div style={{ textAlign: 'center', margin: '20px 0' }}>
          <Button 
            type="primary" 
            size="large" 
            icon={<LinkOutlined />} 
            onClick={generateCode}
            loading={loading}
          >
            Сгенерировать код верификации
          </Button>
        </div>
        
        {verificationCode && (
          <Card type="inner" title="Ваш код верификации:">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Text strong style={{ fontSize: '24px' }}>{verificationCode}</Text>
              <Button 
                icon={<CopyOutlined />} 
                onClick={copyToClipboard}
                type="text"
              />
            </div>
            <Text type="secondary">
              Используйте этот код только в нашем официальном Discord боте!
            </Text>
          </Card>
        )}
      </Space>
    </Card>
  );
};

export default DiscordVerification; 