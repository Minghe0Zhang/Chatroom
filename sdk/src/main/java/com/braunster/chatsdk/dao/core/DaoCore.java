/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package com.braunster.chatsdk.dao.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.braunster.chatsdk.Utils.Debug;
import com.braunster.chatsdk.dao.UserThreadLink;
import com.braunster.chatsdk.dao.UserThreadLinkDao;
import com.braunster.chatsdk.dao.BThread;
import com.braunster.chatsdk.dao.BUser;
import com.braunster.chatsdk.dao.DaoMaster;
import com.braunster.chatsdk.dao.DaoSession;
import com.braunster.chatsdk.dao.entities.Entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import de.greenrobot.dao.Property;
import de.greenrobot.dao.async.AsyncSession;
import de.greenrobot.dao.query.QueryBuilder;
import timber.log.Timber;


/**
 * Manage all creation, deletion and updating Entities.
 */
public class DaoCore {
    private static final String TAG = DaoCore.class.getSimpleName();
    private static final boolean DEBUG = Debug.DaoCore;
    private static final String DB_NAME = "andorid-chatsdk-database";
    private static String dbName;

    public static final int ORDER_ASC = 0;
    public static final int ORDER_DESC = 1;

    private static Context context;

    private static DaoMaster.DevOpenHelper helper;

    @SuppressWarnings("all")
    private static SQLiteDatabase db;
    public static DaoMaster daoMaster;
    public static DaoSession daoSession;
    public static AsyncSession asyncSession;

    /** The property of the "EntityID" of the saved object. This entity comes from the server, For example Firebase server save Entities id's with an Char and Integers sequence.
     * The link between Entities in the databse structure is based on a long id generated by the database automatically.
     * To obtain an Entity using his server id we have to user this property.
     * Each Entity generates its own EntityID property so its very important to save the property id as the first property right after the long id property.
     * A workaround this is available by Checking for certain classes and use a different property for this class.*/
    public final static Property EntityID = new Property(1, String.class, "entityID", false, "ENTITY_ID");

    public static void init(Context ctx) {
        dbName = DB_NAME;
        context = ctx;

        if(helper == null)
            openDB();
    }

    public static void init(Context ctx, String databaseName){
        context = ctx;
        dbName = databaseName;

        if(helper == null)
            openDB();
    }

    private static void openDB(){
        if (context == null)
            throw new NullPointerException("Context is null, Did you initialized DaoCore?");

        helper = new DaoMaster.DevOpenHelper(context, dbName, null);
        db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        asyncSession = daoSession.startAsyncSession();
    }

    public static String generateEntity() {
        return new BigInteger(130, new Random()).toString(32);
    }


    /** Fetch entity for fiven entity ID, If more then one found the first will be returned.*/
    public static <T extends Entity> T fetchEntityWithEntityID(Class<T> c, Object entityID){
        return fetchEntityWithProperty(c, EntityID, entityID);
    }

    /** Fetch an entity for given property and value. If more then one found the first will be returned.*/
    public static <T extends Entity> T fetchEntityWithProperty(Class<T> c, Property property,Object value){
        QueryBuilder<T> qb = daoSession.queryBuilder(c);
        qb.where(property.eq(value));

        List<T> list = qb.list();
        if (list != null && list.size()>0)
            return list.get(0) ;
        else return null;
    }

    public static <T extends Entity> T fetchEntityWithProperties(Class<T> c, Property properties[],Object... values){
        List<T> list = fetchEntitiesWithPropertiesAndOrder(c, null, -1, properties, values);

        if (list == null || list.size() == 0)
            return null;

        return list.get(0);
    }

    /** Fetch a list of entities for a given property and value.*/
    public static <T extends Entity> List<T> fetchEntitiesWithProperty(Class<T> c, Property property, Object value){
        QueryBuilder<T> qb = daoSession.queryBuilder(c);
        qb.where(property.eq(value));
        return qb.list();
    }

    /** Fetch a list of entities for a given properties and values.*/
    public static <T extends Entity> List<T> fetchEntitiesWithProperties(Class<T> c, Property properties[], Object... values){
        return fetchEntitiesWithPropertiesAndOrder(c, null, -1, properties, values);
    }

    /** Fetch a list of entities for a given property and value. Entities are arrange by given order.*/
    public static <T extends Entity> List<T> fetchEntitiesWithPropertyAndOrder(Class<T> c, Property whereOrder, int order, Property property, Object value){
        return fetchEntitiesWithPropertiesAndOrder(c, whereOrder, order, new Property[]{property}, value);
    }

    public static <T extends Entity> List<T>  fetchEntitiesWithPropertiesAndOrder(Class<T> c, Property whereOrder, int order, Property properties[], Object... values){

        if (values == null || properties == null)
            throw new NullPointerException("You must have at least one value and one property");

        if (values.length != properties.length)
            throw new IllegalArgumentException("Values size should match properties size");

        QueryBuilder<T> qb = daoSession.queryBuilder(c);
        qb.where(properties[0].eq(values[0]));

        for (int i = 0 ; i < values.length ; i++)
            qb.where(properties[i].eq(values[i]));

        if (whereOrder != null && order != -1)
            switch (order)
            {
                case ORDER_ASC:
                    qb.orderAsc(whereOrder);
                    break;

                case ORDER_DESC:
                    qb.orderDesc(whereOrder);
                    break;
            }

        return qb.list();
    }

    public static <T extends Entity> List<T>  fetchEntitiesWithPropertiesAndOrderAndLimit(Class<T> c, int limit, Property whereOrder, int order, Property properties[], Object... values){

        if (values == null || properties == null)
            throw new NullPointerException("You must have at least one value and one property");

        if (values.length != properties.length)
            throw new IllegalArgumentException("Values size should match properties size");

        QueryBuilder<T> qb = daoSession.queryBuilder(c);
        qb.where(properties[0].eq(values[0]));

        if (values.length > 1)
            for (int i = 0 ; i < values.length ; i++)
                qb.where(properties[i].eq(values[i]));

        if (whereOrder != null && order != -1)
            switch (order)
            {
                case ORDER_ASC:
                    qb.orderAsc(whereOrder);
                    break;

                case ORDER_DESC:
                    qb.orderDesc(whereOrder);
                    break;
            }

        if (limit != -1)
            qb.limit(limit);

        return qb.listLazy();
    }

    public static <T extends Entity> T fetchOrCreateEntityWithEntityID(Class<T> c, String entityId){
        if (DEBUG) Timber.v("fetchOrCreateEntityWithEntityID, EntityID: %s", entityId);

        T entity = fetchEntityWithEntityID(c, entityId);

        if (entity == null)
        {
            entity = getEntityForClass(c);

            entity.setEntityID(entityId);

            entity = createEntity(entity);
        }

        return entity;
    }

    /** Fetch an Entity for given property and class.
     * If no Entity found a new one will be created.
     * The calling method have to handle the the inserting
     * of the given value if a new Entity was created.
     *
     * @return and object that Extends the Entity object.
     * The object will be created from the given class.*/


    @SuppressWarnings("unchecked") private static <T extends Entity> T fetchOrCreateEntityWithProperty(Class<T> c, Property property, Object value){
        if (DEBUG) Timber.v("fetchOrCreateEntityWithProperty, Value: %s", value);
        T entity = fetchEntityWithProperty(c, property, value);

        if (entity != null)
            return entity;

        // Create the new entity.
        Class<T> clazz = null;
        T o = null;
        try {
            clazz = (Class<T>) Class.forName(c.getName());
            Constructor<T> ctor =  clazz.getConstructor(c);
            o = (T) ctor.newInstance();
        } catch (ClassNotFoundException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("ClassNotFoundException");
        } catch (NoSuchMethodException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("NoSuchMethodException");
        } catch (InvocationTargetException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("InvocationTargetException");
        } catch (InstantiationException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("InstantiationException");
        } catch (IllegalAccessException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("IllegalAccessException");
        }

        if (o != null)
        {
            return createEntity(o);
        }

        return null;
    }

    /* Update, Create and Delete*/
    public static  <T extends Entity> T createEntity(T entity){
        if (DEBUG) Timber.v("createEntity");

        if (entity == null)
        {
            return null;
        }


        daoSession.insertOrReplace(entity);

        if(DEBUG) printEntity(entity);

        return entity;
    }

    public static <T extends Entity> T deleteEntity(T entity){
        if (DEBUG) Timber.v("deleteEntity");

        if (entity == null)
        {
            if (DEBUG) Timber.e("Entity is null");
            return null;
        }


        if(DEBUG) printEntity(entity);

        daoSession.delete(entity);

        daoSession.clear();

        return entity;
    }

    public static <T extends Entity> T updateEntity(T entity){
        if (DEBUG) Timber.v("updateEntity");

        if (entity==null)
            return null;

        if (DEBUG) printEntity(entity);

        daoSession.update(entity);

        return entity;
    }

    public static void printEntity(Entity entity){

    }

    public static void connectUserAndThread(BUser user, BThread thread){
        if (DEBUG) Timber.v("connectUserAndThread, User ID: %s, Name: %s, ThreadID: %s",  + user.getId(), user.getMetaName(), thread.getId());
        UserThreadLink linkData = new UserThreadLink();
        linkData.setBThreadDaoId(thread.getId());
        linkData.setBThread(thread);
        linkData.setBUserDaoId(user.getId());
        linkData.setBUser(user);
        createEntity(linkData);
    }

    public static void breakUserAndThread(BUser user, BThread thread){
        if (DEBUG) Timber.v("breakUserAndThread, User ID: %s, Name: %s, ThreadID: %s",  + user.getId(), user.getMetaName(), thread.getId());
        UserThreadLink linkData = fetchEntityWithProperties(UserThreadLink.class, new Property[] {UserThreadLinkDao.Properties.BThreadDaoId, UserThreadLinkDao.Properties.BUserDaoId}, thread.getId(), user.getId());
        DaoCore.deleteEntity(linkData);
    }

    @SuppressWarnings("unchecked") private static <T extends Entity> T getEntityForClass(Class<T> c){
        // Create the new entity.
        Class<T> clazz = null;
        T o = null;
        try {
            clazz = (Class<T>) Class.forName(c.getName());
            Constructor<T> ctor = clazz.getConstructor();
            o = ctor.newInstance();
        } catch (ClassNotFoundException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("ClassNotFoundException");
        } catch (NoSuchMethodException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("NoSuchMethodException");
        } catch (InvocationTargetException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("InvocationTargetException");
        } catch (InstantiationException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("InstantiationException");
        } catch (IllegalAccessException e) {
//                e.printStackTrace();
            if (DEBUG) Timber.e("IllegalAccessException");
        }

        return o;
    }
}
