package com.xiaomaigou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xiaomaigou.page.service.ItemPageService;
import com.xiaomaigou.pojo.TbGoods;
import com.xiaomaigou.pojo.TbItem;
import com.xiaomaigou.pojogroup.Goods;
import com.xiaomaigou.search.service.ItemSearchService;
import com.xiaomaigou.sellergoods.service.GoodsService;
import entity.PageResult;
import entity.Result;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Controller
 *
 * @author root
 */
//此处使用RestController，RestController相当于@Controller和@ResponseBody，这样就不用在每个方法上都写了@ResponseBody，这样可以少写很多代码，@ResponseBody表示该返回值为直接输出，如果不加则表示返回的是页面
@RestController
@RequestMapping("/goods")
public class GoodsController {

    //注意：这里必须使用com.alibaba.dubbo.config.annotation.Reference;因为它远程调用，而不是本地调用，不能使用@Autowired注入，也叫远程注入
    @Reference
    private GoodsService goodsService;

    @Reference(timeout=100000)
    private ItemSearchService itemSearchService;

    @Reference(timeout=40000)
    private ItemPageService itemPageService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @RequestMapping("/findAll")
    public List<TbGoods> findAll() {
        return goodsService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @RequestMapping("/findPage")
    public PageResult findPage(int page, int rows) {
        return goodsService.findPage(page, rows);
    }

    /**
     * 增加，运营商无需增加商品
     *
     * @param goods
     * @return
     */
//    @RequestMapping("/add")
//    public Result add(@RequestBody TbGoods goods) {
//        try {
//            goodsService.add(goods);
//            return new Result(true, "增加成功");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new Result(false, "增加失败");
//        }
//    }

    /**
     * 修改
     *
     * @param goods
     * @return
     */
    @RequestMapping("/update")
    public Result update(@RequestBody Goods goods) {
        try {
            goodsService.update(goods);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "修改失败");
        }
    }

    /**
     * 获取实体
     *
     * @param id
     * @return
     */
    @RequestMapping("/findOne")
    public Goods findOne(Long id) {
        return goodsService.findOne(id);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @RequestMapping("/delete")
    public Result delete(Long[] ids) {
        try {
            goodsService.delete(ids);
            //根据id从索引库中删除
            itemSearchService.deleteByGoodsIds(Arrays.asList(ids));
            return new Result(true, "删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param page
     * @param rows
     * @return
     */
    @RequestMapping("/search")
    public PageResult search(@RequestBody TbGoods goods, int page, int rows) {
        return goodsService.findPage(goods, page, rows);
    }

    /**
     * 更新商品状态（商家审核）
     * @param ids
     * @param status
     * @return
     */
    @RequestMapping("/updateStatus")
    public Result updateStatus(Long[] ids, String status){
        try {
            goodsService.updateStatus(ids, status);
            //按照SPU ID查询 SKU列表(状态为1，即启用，而不是下架或者删除)
            if("1".equals(status)){//如果是审核通过
                //得到需要导入的SKU列表
                List<TbItem> itemList = goodsService.findItemListByGoodsIdListAndStatus(ids, status);
                //调用搜索接口实现数据批量导入
                if(itemList.size()>0){
                    //导入到solr
                    itemSearchService.importList(itemList);
                }else{
                    System.out.println("没有明细数据导入！");
                }

                // 生成商品html详细静态页面
                for(Long goodsId:ids){
                    itemPageService.genItemHtml(goodsId);
                }
            }

            return new Result(true, "更新状态成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "更新状态失败");
        }
    }

    // 生成html静态页面
//    @RequestMapping("/genHtml")
//    public void genHtml(Long goodsId){
//
//        itemPageService.genItemHtml(goodsId);
//
//    }

}
