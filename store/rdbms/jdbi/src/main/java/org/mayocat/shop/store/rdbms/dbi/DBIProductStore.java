package org.mayocat.shop.store.rdbms.dbi;

import java.util.List;

import javax.inject.Inject;

import org.mayocat.shop.model.Category;
import org.mayocat.shop.model.Product;
import org.mayocat.shop.store.EntityAlreadyExistsException;
import org.mayocat.shop.store.EntityDoesNotExistException;
import org.mayocat.shop.store.HasOrderedCollections;
import org.mayocat.shop.store.InvalidEntityException;
import org.mayocat.shop.store.InvalidMoveOperation;
import org.mayocat.shop.store.ProductStore;
import org.mayocat.shop.store.StoreException;
import org.mayocat.shop.store.rdbms.dbi.dao.ProductDAO;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

@Component(hints = { "jdbi", "default" })
public class DBIProductStore extends AbstractEntityStore implements ProductStore, Initializable
{
    private static final String PRODUCT_POSITION = "product.position";

    @Inject
    private DBIProvider dbi;

    private static final String PRODUCT_TABLE_NAME = "product";

    private ProductDAO dao;

    public void create(Product product) throws EntityAlreadyExistsException, InvalidEntityException
    {
        if (this.dao.findBySlug(product.getSlug(), getTenant()) != null) {
            throw new EntityAlreadyExistsException();
        }

        this.dao.begin();

        Long entityId = this.dao.createEntity(product, PRODUCT_TABLE_NAME, getTenant());
        Integer lastIndex = this.dao.lastPosition(getTenant());
        if (lastIndex == null) {
            lastIndex = 0;
        }
        this.dao.create(entityId, lastIndex + 1, product);
        this.dao.insertTranslations(entityId, product.getTranslations());

        this.dao.commit();
    }

    @Override
    public void update(Product product) throws EntityDoesNotExistException, InvalidEntityException
    {
        this.dao.begin();

        Product originalProduct = this.dao.findBySlug(product.getSlug(), getTenant());
        if (originalProduct == null) {
            this.dao.commit();
            throw new EntityDoesNotExistException();
        }
        product.setId(originalProduct.getId());
        Integer updatedRows = this.dao.update(product);

        this.dao.commit();

        if (updatedRows <= 0) {
            throw new StoreException("No rows was updated when updating product");
        }
    }

    public void moveProduct(String productToMove, String productToMoveRelativeTo,
            HasOrderedCollections.RelativePosition relativePosition) throws InvalidMoveOperation
    {
        this.dao.begin();

        List<Product> allProducts = this.findAll();
        MoveEntityInListOperation<Product> moveOp =
                new MoveEntityInListOperation<Product>(allProducts, productToMove,
                        productToMoveRelativeTo, relativePosition);

        if (moveOp.hasMoved()) {
            this.dao.updatePositions(PRODUCT_TABLE_NAME, moveOp.getEntities(), moveOp.getPositions());
        }

        this.dao.commit();
    }

    public List<Product> findAll()
    {
        return this.dao.findAll(PRODUCT_TABLE_NAME, PRODUCT_POSITION, getTenant());
    }

    public List<Product> findAll(Integer number, Integer offset)
    {
        return this.dao.findAll(PRODUCT_TABLE_NAME, PRODUCT_POSITION, getTenant(), number, offset);
    }

    @Override
    public Product findById(Long id)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.dao = this.dbi.get().onDemand(ProductDAO.class);
    }

    public Product findBySlug(String slug)
    {
        return this.dao.findBySlugWithTranslations(PRODUCT_TABLE_NAME, slug, getTenant());
    }

    @Override
    public List<Product> findAllInCategory(Category category, int number, int offset)
    {
        // TODO Auto-generated method stub
        return null;
    }
}