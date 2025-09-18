package com.viglet.turing.commons.se;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TurSEParameters implements Serializable {
        private String query;
        private TurSNFilterParams turSNFilterParams;
        private List<String> boostQueries;
        private Integer currentPage;
        private String sort;
        private Integer rows;
        private String group;
        private Integer autoCorrectionDisabled;
        private TurSNSitePostParamsBean turSNSitePostParamsBean;

        public TurSEParameters(TurSNSearchParams turSNSearchParams) {
                this(turSNSearchParams, null);
        }

        public TurSEParameters(TurSNSearchParams turSNSearchParams,
                        TurSNSitePostParamsBean gTurSNSitePostParamsBean) {
                super();
                TurSNFilterParams turSNFilterParams = TurSNFilterParams.builder()
                                .defaultValues(turSNSearchParams.getFq())
                                .and(turSNSearchParams.getFqAnd()).or(turSNSearchParams.getFqOr())
                                .operator(turSNSearchParams.getFqOp())
                                .itemOperator(turSNSearchParams.getFqiOp()).build();

                this.query = turSNSearchParams.getQ();
                this.turSNFilterParams = turSNFilterParams;
                this.currentPage = turSNSearchParams.getP();
                this.sort = turSNSearchParams.getSort();
                this.rows = turSNSearchParams.getRows();
                this.group = turSNSearchParams.getGroup();
                this.autoCorrectionDisabled = turSNSearchParams.getNfpr();
                this.turSNSitePostParamsBean = gTurSNSitePostParamsBean;
                overrideFromPost(gTurSNSitePostParamsBean);
        }

        private void overrideFromPost(TurSNSitePostParamsBean gTurSNSitePostParamsBean) {
                Optional.ofNullable(gTurSNSitePostParamsBean).ifPresent(postParams -> {
                        setSort(Optional.ofNullable(postParams.getSort()).orElse(getSort()));
                        setRows(Optional.ofNullable(postParams.getRows()).orElse(getRows()));
                        setGroup(Optional.ofNullable(postParams.getGroup()).orElse(getGroup()));
                        setCurrentPage(Optional.ofNullable(postParams.getPage())
                                        .orElse(getCurrentPage()));
                        setQuery(Optional.ofNullable(postParams.getQuery()).orElse(getQuery()));
                        getTurSNFilterParams().setDefaultValues(
                                        Optional.ofNullable(postParams.getFq()).orElse(
                                                        getTurSNFilterParams().getDefaultValues()));
                        getTurSNFilterParams().setAnd(Optional.ofNullable(postParams.getFqAnd())
                                        .orElse(getTurSNFilterParams().getAnd()));
                        getTurSNFilterParams().setOr(Optional.ofNullable(postParams.getFqOr())
                                        .orElse(getTurSNFilterParams().getOr()));
                        getTurSNFilterParams().setOperator(
                                        Optional.ofNullable(postParams.getFqOperator()).orElse(
                                                        getTurSNFilterParams().getOperator()));
                });
        }
}
