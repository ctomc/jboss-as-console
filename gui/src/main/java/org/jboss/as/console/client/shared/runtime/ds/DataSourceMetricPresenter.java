package org.jboss.as.console.client.shared.runtime.ds;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.dispatch.DispatchAsync;
import org.jboss.as.console.client.shared.dispatch.impl.DMRAction;
import org.jboss.as.console.client.shared.dispatch.impl.DMRResponse;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.state.CurrentServerSelection;
import org.jboss.as.console.client.shared.state.ServerSelectionEvent;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

/**
 * @author Heiko Braun
 * @date 12/19/11
 */
public class DataSourceMetricPresenter extends Presenter<DataSourceMetricPresenter.MyView, DataSourceMetricPresenter.MyProxy>
        implements ServerSelectionEvent.ServerSelectionListener{

    private final PlaceManager placeManager;
    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private CurrentServerSelection serverSelection;
    private DataSource selectedDS;
    private BeanFactory factory;
    private EntityAdapter<DataSource> dataSourceAdapter;

    private LoadDataSourceCmd loadDSCmd;

    @ProxyCodeSplit
    @NameToken(NameTokens.DataSourceMetricPresenter)
    public interface MyProxy extends Proxy<DataSourceMetricPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(DataSourceMetricPresenter presenter);
        void clearSamples();
        void setDatasources(List<DataSource> datasources);
        void setDSPoolMetric(Metric poolMetric);
        void setDSCacheMetric(Metric metric);
    }

    @Inject
    public DataSourceMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager,  DispatchAsync dispatcher,
            ApplicationMetaData metaData, RevealStrategy revealStrategy,
            CurrentServerSelection serverSelection, BeanFactory factory) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.serverSelection = serverSelection;
        this.factory = factory;

        this.loadDSCmd = new LoadDataSourceCmd(dispatcher, metaData);

    }

    @Override
    public void onServerSelection(String hostName, String serverName) {

        getView().clearSamples();

        // refresh if needed. Otherwise it will happen onReset()
        if(isVisible()) refresh();
    }

    private void refresh() {
        loadDSCmd.execute(new SimpleCallback<List<DataSource>>() {
            @Override
            public void onSuccess(List<DataSource> result) {
                getView().setDatasources(result);
            }
        });
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(ServerSelectionEvent.TYPE, this);
    }


    @Override
    protected void onReset() {
        super.onReset();
        refresh();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    public void setSelectedDS(DataSource currentSelection) {
        this.selectedDS = currentSelection;
        if(selectedDS!=null)
            loadDSPoolMetrics();
    }

    private void loadDSPoolMetrics() {
        if(null==selectedDS)
            throw new RuntimeException("DataSource selection is null!");

        getView().clearSamples();

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "datasources");
        operation.get(ADDRESS).add("data-source", selectedDS.getName());
        operation.get(ADDRESS).add("statistics", "pool");

        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error("Error loading metrics", response.getFailureDescription());
                }
                else
                {
                    ModelNode result = response.get(RESULT).asObject();

                    long avail = result.get("AvailableCount").asLong();
                    long active = result.get("ActiveCount").asLong();
                    long max = result.get("MaxUsedCount").asLong();

                    Metric poolMetric = new Metric(
                            avail,active,max
                    );

                    getView().setDSPoolMetric(poolMetric);
                }
            }
        });
    }

     private void loadDSCacheMetrics() {
        if(null==selectedDS)
            throw new RuntimeException("DataSource selection is null!");

        getView().clearSamples();

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "datasources");
        operation.get(ADDRESS).add("data-source", selectedDS.getName());
        operation.get(ADDRESS).add("statistics", "jdbc");

        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error("Error loading metrics", response.getFailureDescription());
                }
                else
                {
                    ModelNode result = response.get(RESULT).asObject();

                    long size = result.get("PreparedStatementCacheCurrentSize").asLong();
                    long hit = result.get("PreparedStatementCacheHitCount").asLong();
                    long miss = result.get("PreparedStatementCacheMissCount").asLong();

                    Metric metric = new Metric(
                            size,hit,miss
                    );

                    getView().setDSCacheMetric(metric);
                }
            }
        });
    }
}
