package com.viglet.turing.connector.plugin.webcrawler.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@JsonIgnoreProperties({ "turWCSource" })
public class TurWCAllowUrl extends TurWCUrl {

    @Serial
    private static final long serialVersionUID = 1L;

    // bi-directional many-to-one association to TurWCSource
    @ManyToOne
    @JoinColumn(name = "wc_source_id", nullable = false)
    private TurWCSource turWCSource;

    public TurWCAllowUrl(String url, TurWCSource turWCSource) {
        this.url = url;
        this.turWCSource = turWCSource;
    }
}
