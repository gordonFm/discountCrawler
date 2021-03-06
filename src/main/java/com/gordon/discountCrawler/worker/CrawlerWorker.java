package com.gordon.discountCrawler.worker;

import com.gordon.discountCrawler.constants.CrawlerParams;
import com.gordon.discountCrawler.fetcher.PageFetcher;
import com.gordon.discountCrawler.filter.ProductFilter;
import com.gordon.discountCrawler.model.DiscountProduct;
import com.gordon.discountCrawler.parser.ContentParser;
import com.gordon.discountCrawler.queue.FilteredDiscountProductQueue;
import com.gordon.discountCrawler.storage.DataStorage;
import com.gordon.discountCrawler.storage.impl.ListStorage;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by wwz on 2016/2/22.
 */
public class CrawlerWorker implements Runnable {
    private static final Logger Log = Logger.getLogger(CrawlerWorker.class.getName());

    private PageFetcher pageFetcher = new PageFetcher();

    private ContentParser contentParser = new ContentParser();

    private DataStorage dataStorage = ListStorage.getInstance();

    private int threadIndex;

    private Integer page = 1;


    public CrawlerWorker(int i) {
        this.threadIndex = i;
    }

    public void startCrawl() {
        while (true) {
            List<DiscountProduct> discountProductList =
                    contentParser.parseHTML(pageFetcher.getContentFromUrl(CrawlerParams.PAGE_URL + page.toString()));
            //当抓取页面的element不为空时抓取
            if (discountProductList.size() == 0) {
//            if (page == 5) {
                break;
            }
            dataStorage.store(discountProductList);
            try {
                Thread.sleep(CrawlerParams.INIT_REQUEST_DELAY_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            page++;
        }
        pageFetcher.close();
    }

    /**
     * 运行该线程爬取更新的商品信息
     */
    public void run() {
        Integer page = 1;
        while (true) {
            PageFetcher pageFetcher = new PageFetcher();
            List<DiscountProduct> newDetectedList =
                    contentParser.parseHTML(pageFetcher.getContentFromUrl(CrawlerParams.PAGE_URL + Integer.toString(page)));
            pageFetcher.close();
            if (newDetectedList == null) {
                continue;
            }
            //当前页面没有新的商品信息更新
            if (ListStorage.getDiscountProductList().containsAll(newDetectedList)) {
                //线程等待后继续抓取页面
                try {
                    Thread.sleep(CrawlerParams.REQUEST_DELAY_TIME);
                    //将抓取页面重置为第一页
                    page = 1;
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //当前页面有新的商品信息更新
            for (DiscountProduct discountProduct : newDetectedList) {
                //筛选更新的商品信息
                if (!ListStorage.getDiscountProductList().contains(discountProduct)) {
                    //将关键字匹配的商品信息加入队列
                    if (ProductFilter.isMatch(discountProduct)) {
                        FilteredDiscountProductQueue.addElement(discountProduct);
                    }
                    //将更新的商品信息加入保存商品信息的集合
                    ListStorage.getDiscountProductList().add(discountProduct);
                }
            }
            try {
                Thread.sleep(CrawlerParams.REQUEST_DELAY_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //页面+1，抓取下一页
            page++;
        }
    }
}
