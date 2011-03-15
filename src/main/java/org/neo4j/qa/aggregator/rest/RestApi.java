package org.neo4j.qa.aggregator.rest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.neo4j.qa.aggregator.model.StatisticRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("/")
@Component
@Scope("request")
public class RestApi {
// ------------------------------ FIELDS ------------------------------

    @Autowired private EntityManagerFactory emf;

// -------------------------- OTHER METHODS --------------------------

    @GET
    @Path("/{component}/{version}")
    @Produces("application/json")
    @Transactional
    public Response get(@PathParam("component") final String component,
                        @PathParam("version") final String version) throws JSONException {
        try {
            return (Response) new JpaTemplate(emf).execute(new JpaCallback<Object>() {
                public Object doInJpa(EntityManager entityManager) throws PersistenceException {
                    TypedQuery<StatisticRecord> q = entityManager.createQuery("FROM StatisticRecord " +
                            " WHERE component=:component AND version=:version order by timestamp desc", StatisticRecord.class);
                    q.setParameter("component", component);
                    q.setParameter("version", version);
                    q.setMaxResults(10);

                    try {
                        JSONWriter a = new JSONStringer().array();
                        for (StatisticRecord statisticRecord : q.getResultList()) {
                            a = a.object()
                                    .key("timestamp").value(statisticRecord.getTimestamp())
                                    .key("data").value(statisticRecord.getValues()).endObject();
                        }
                        a.endArray();
                        return Response.status(Response.Status.OK).entity(a.toString()).build();
                    } catch (JSONException e) {
                        throw new PersistenceException(e);
                    }
                }
            });
        } catch (Exception e) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JSONStringer().object().key("message").value(e.getMessage()).endObject().toString())
                    .build();
        }
    }


    @GET
    @Path("/graph/{component}/{version}")
    @Produces("text/plain")
    @Transactional
    public Response getChart(@PathParam("component") final String component,
                             @PathParam("version") final String version) throws JSONException {

        return (Response) new JpaTemplate(emf).execute(new JpaCallback<Object>() {
            public Object doInJpa(EntityManager entityManager) throws PersistenceException {
                TypedQuery<StatisticRecord> q = entityManager.createQuery("FROM StatisticRecord " +
                        " WHERE component=:component AND version=:version order by timestamp desc", StatisticRecord.class);
                q.setParameter("component", component);
                q.setParameter("version", version);
                q.setMaxResults(20);

                StringBuilder s = new StringBuilder();
                List<String> keys = new ArrayList<String>();
                for (StatisticRecord statisticRecord : q.getResultList()) {
                    for (String s1 : statisticRecord.getValues().keySet()) {
                        if (!keys.contains(s1)) {
                            keys.add(s1);
                        }
                    }
                }

                s.append("Timestamp");
                for (String key : keys) {
                    s.append(",").append(key);
                }
                s.append("\n");

                for (StatisticRecord statisticRecord : q.getResultList()) {
                    s.append(new SimpleDateFormat("MM/dd/yy HH:mm").format(  statisticRecord.getTimestamp()));

                    for (String key : keys) {
                        Double v = statisticRecord.getValues().get(key);
                        s.append(",").append(v != null ? v : "");
                    }
                    s.append("\n");

                }
                return Response.status(Response.Status.OK).entity(s.toString()).build();


            }
        });


    }

    @GET
    @Path("/graph")
    @Produces("application/json")
    @Transactional
    public Response getChart() throws JSONException {
        try {
            return (Response) new JpaTemplate(emf).execute(new JpaCallback<Object>() {
                public Object doInJpa(EntityManager entityManager) throws PersistenceException {
                    try {

                        Query q = entityManager.createQuery("SELECT component,version FROM StatisticRecord " +
                                " group by component,version");

                        JSONWriter res = new JSONStringer().array();

                        List<Object[]> resultList = q.getResultList();
                        for (Object[] o : resultList) {
                            res.value(o[0] + "/" + o[1]);
                        }
                        res.endArray();

                        return Response.status(Response.Status.OK).entity(res.toString()).build();

                    } catch (JSONException e) {
                        throw new PersistenceException(e);
                    }
                }
            });

        } catch (Exception e) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JSONStringer().object().key("message").value(e.getMessage()).endObject().toString())
                    .build();
        }
    }

    @POST
    @Path("/{component}/{version}")
    @Produces("application/json")
    @Transactional
    public Response insert(@PathParam("component") final String component,
                           @PathParam("version") final String version,
                           final String body) throws JSONException {
        try {
            final JSONObject o = new JSONObject(body);
            final Map<String, Double> values = new HashMap<String, Double>();
            final Iterator<String> iterator = o.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                values.put(key, o.getDouble(key));
            }

            new JpaTemplate(emf).persist(new StatisticRecord(component, version, values));

            return Response.status(Response.Status.OK).build();
        } catch (JSONException e) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JSONStringer().object().key("message").value(e.getMessage()).endObject().toString())
                    .build();
        }
    }
}