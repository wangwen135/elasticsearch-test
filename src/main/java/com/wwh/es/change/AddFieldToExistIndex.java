package com.wwh.es.change;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

/**
 * <pre>
 *  添加或者修改字段
 *  经过测试的
 * </pre>
 * 
 * @author wwh
 * @date 2017年1月3日 下午5:11:53
 */
public class AddFieldToExistIndex {

    private static final String indexName = "dwd-p1";
    // private static final String indexName = "bdmi4";

    private static final String typeName = "dwdata";
    // private static final String typeName = "p1";

    /**
     * 一批获取数据
     */
    private static final int BATCH_SIZE = 500;

    public static void main(String[] args) throws UnknownHostException {

        Settings settings = Settings.settingsBuilder().put("cluster.name", "hinge-es").build();
        TransportClient client = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("192.168.1.91"), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("192.168.1.92"), 9300));

        // .setNoFields().
        SearchResponse scrollResp = client.prepareSearch(indexName).setTypes(typeName).setQuery(QueryBuilders.matchAllQuery()).setNoFields().setSize(BATCH_SIZE)
                .setScroll(new TimeValue(600000)).execute().actionGet();

        long totalSize = 0;

        while (true) {

            BulkRequestBuilder bulkRequest = client.prepareBulk();

            for (SearchHit hit : scrollResp.getHits().getHits()) {
                // 循环遍历
                totalSize++;
                String id = hit.getId();
                // 给每一个文档增加一个字段
                UpdateRequest ur = new UpdateRequest(indexName, typeName, id).doc("fid", id);

                bulkRequest.add(ur);
            }

            BulkResponse bulkResponse = bulkRequest.get();

            if (bulkResponse.hasFailures()) {
                System.err.println("############ 出错了！！！！！");
            }

            System.out.println("总数：" + totalSize);

            scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();

            if (scrollResp.getHits().getHits().length == 0) {
                break;
            }
        }

        System.out.println("结束");

    }
}
