package no.nav.foreldrepenger.web.app.tjenester.formidling;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spesifikasjon for å hente opp et BrevGrunnlag.
 * Merk at props her kan ekskludere/kombineres.
 * Må minimum angi en behandling referanse.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
@JsonInclude(value = Include.NON_ABSENT, content = Include.NON_EMPTY)
public class BrevGrunnlagRequest {

    @JsonProperty("dataset")
    @Valid
    public Set<Dataset> dataset = EnumSet.of(Dataset.BEHANDLING, Dataset.FAGSAK, Dataset.VERGE, Dataset.BEHANDLING_RESULTAT);
    /**
     * Forespørsel på behandling referanse gir kun siste grunnlag.
     */
    @JsonProperty("behandlingReferanse")
    @Valid
    private UUID behandlingReferanse;

    protected BrevGrunnlagRequest() {
        // default ctor.
    }

    @JsonCreator
    public BrevGrunnlagRequest(@JsonProperty(value = "behandlingReferanse", required = true) @Valid @NotNull UUID behandlingReferanse) {
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse, "behandlingReferanse");
    }

    @AssertTrue(message = "behandlingReferanse må spesifiseres")
    private boolean isOk() {
        return behandlingReferanse != null;
    }

    public BrevGrunnlagRequest medDataset(Dataset data) {
        this.dataset.add(data);
        return this;
    }

    public BrevGrunnlagRequest medDataset(Collection<Dataset> data) {
        this.dataset = Set.copyOf(data);
        return this;
    }

    public BrevGrunnlagRequest forBehandling(UUID behandlingReferanse) {
        this.behandlingReferanse = behandlingReferanse;
        return this;
    }

    public Set<Dataset> getDataset() {
        return dataset;
    }

    public UUID getBehandlingReferanse() {
        return behandlingReferanse;
    }
}
