package Q12
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object Q12 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String]): RDD[_] = {
    val web_sales = input1
    val date_dim = input2
    val item = input3

    val filtered_item = item.filter {
      row =>
        val category = row.split(",")(12)
        category.equals("Home") || category.equals("Electronics") || category.equals("Shoes")
    }
    filtered_item.take(10).foreach(println)

    val filtered_dd = date_dim.filter {
      row =>
        val d_date = row.split(",")(2)
        d_date.substring(0, 7).equals("1999-01")
        // isBetween(d_date, START_DATE, END_DATE)
    }
    // filtered_dd.take(10).foreach(println)

    val map1 = web_sales.map(row => (row.split(",")(2)/*ws_item_sk*/, row))
    val map7 = filtered_item.map(row => (row.split(",")(0), row))
    val join1 = map1.join(map7)
    // join1.take(10).foreach(println)

    val map2 = join1.map {
        row =>
          val ws_row = row._2._1
          val i_row = row._2._2
        // case (item_sk, (ws_row, i_row)) =>
          (ws_row.split(",")(33)/*ws_sold_date*/, (ws_row, i_row))
      }
    val map8 = filtered_dd.map(row => (row.split(",")(0), row))
    val join2 = map2.join(map8)
    // join2.take(10).foreach(println)

    val map3 = join2.map {
        row =>
          val ws_row = row._2._1._1
          val i_row = row._2._1._2
          val dd_row = row._2._2
        // case (_, ((ws_row, i_row), dd_row)) =>
          val i_item_id = i_row.split(",")(1)
          val i_item_desc = i_row.split(",")(4)
          val i_category = i_row.split(",")(12)
          val i_class = i_row.split(",")(10)
          val i_current_price = i_row.split(",")(5)
          val ws_ext_sales_price = ws_row.split(",")(22).toInt //convertColToFloat(ws_row, 22) 

          ((i_item_id, i_item_desc, i_category, i_class, i_current_price), ws_ext_sales_price) // there should be another value here
      }

      /*
    val map4 = map3.map {
        case ((i_item_id, i_item_desc, i_category, i_class, i_current_price), ws_ext_sales_price) =>
          (i_class, ws_ext_sales_price)
      }
    val rbk1 = map4.reduceByKey(_+_)*/
    // rbk1.take(10).foreach(println)

    val rbk2 = map3.reduceByKey(_ + _)
    // rbk2.take(10).foreach(println)
    rbk2

    /*
    val map5 = rbk2.map {
        case ((i_item_id, i_item_desc, i_category, i_class, i_current_price), ws_ext_sales_price) =>
          (i_class, (i_item_id, i_item_desc, i_category, i_current_price, ws_ext_sales_price))
      }
    val join3 = map5.join(rbk1)
    join3.take(10).foreach(println)

    val map6 = join3.map {
        case (i_class, ((i_item_id, i_item_desc, i_category, i_current_price, ws_ext_sales_price), class_rev)) =>
          (i_item_id, i_item_desc, i_category, i_class, i_current_price, ws_ext_sales_price, ws_ext_sales_price/class_rev)
      }
    map6*/
    // val sortBy1 = map6.sortBy(_._3)

    // sortBy1.take(10).foreach(println)

    /*

    define YEAR=random(1998,2002,uniform);
    define SDATE=date([YEAR]+"-01-01",[YEAR]+"-07-01",sales);
    define CATEGORY=ulist(dist(categories,1,1),3);
    define _LIMIT=100;

    [_LIMITA] select [_LIMITB] i_item_id
          ,i_item_desc
          ,i_category
          ,i_class
          ,i_current_price
          ,sum(ws_ext_sales_price) as itemrevenue
          ,sum(ws_ext_sales_price)*100/sum(sum(ws_ext_sales_price)) over
              (partition by i_class) as revenueratio
    from
      web_sales
          ,item
          ,date_dim
    where
      ws_item_sk = i_item_sk
        and i_category in ('[CATEGORY.1]', '[CATEGORY.2]', '[CATEGORY.3]')
        and ws_sold_date_sk = d_date_sk
      and d_date between cast('[SDATE]' as date)
            and (cast('[SDATE]' as date) + 30 days)
    group by
      i_item_id
            ,i_item_desc
            ,i_category
            ,i_class
            ,i_current_price
    order by
      i_category
            ,i_class
            ,i_item_id
            ,i_item_desc
            ,revenueratio
    [_LIMITC];
    */

  }
}