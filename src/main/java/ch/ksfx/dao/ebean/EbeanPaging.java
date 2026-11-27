package ch.ksfx.dao.ebean;

import io.ebean.ExpressionList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** A Spring Data page from an Ebean query - the paging/sorting the list DAOs each repeated inline. */
public final class EbeanPaging
{
    private EbeanPaging()
    {
    }

    /** Applies the pageable's sort (or {@code defaultOrder} ascending when unsorted), offset and size. */
    public static <T> Page<T> page(ExpressionList<T> expressionList, Pageable pageable, String defaultOrder)
    {
        boolean sorted = false;

        for (Sort.Order order : pageable.getSort()) {
            if (!order.getProperty().equals("UNSORTED")) {
                if (order.isAscending()) {
                    expressionList.order().asc(order.getProperty());
                } else {
                    expressionList.order().desc(order.getProperty());
                }

                sorted = true;
            }
        }

        if (!sorted && defaultOrder != null) {
            expressionList.order().asc(defaultOrder);
        }

        expressionList.setFirstRow((int) pageable.getOffset());
        expressionList.setMaxRows(pageable.getPageSize());

        return new PageImpl<>(expressionList.findList(), pageable, expressionList.findCount());
    }
}
