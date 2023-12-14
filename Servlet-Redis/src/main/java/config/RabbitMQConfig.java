package config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import util.ChannelPooledObjectFactory;
import util.Constants;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author xiaorui
 */
public class RabbitMQConfig {
    private Connection rabbitmqConnection;
    private GenericObjectPool<Channel> channelPool;

    public RabbitMQConfig() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(Constants.MQ_HOST);
        connectionFactory.setPort(Constants.MQ_PORT);
        connectionFactory.setUsername(Constants.MQ_USER);
        connectionFactory.setPassword(Constants.MQ_PWD);
        connectionFactory.setVirtualHost(Constants.MQ_V_HOST);

        rabbitmqConnection = connectionFactory.newConnection();


        // Setting up the channel pool
        ChannelPooledObjectFactory pooledObjectFactory = new ChannelPooledObjectFactory(rabbitmqConnection);
        GenericObjectPoolConfig<Channel> config = new GenericObjectPoolConfig<>();
        this.channelPool = new GenericObjectPool<>(pooledObjectFactory, config);
    }

    public Connection getConnection() {
        return rabbitmqConnection;
    }

    public Channel borrowChannel() throws Exception {
        return channelPool.borrowObject();
    }

    public void returnChannel(Channel channel) {
        channelPool.returnObject(channel);
    }
}
