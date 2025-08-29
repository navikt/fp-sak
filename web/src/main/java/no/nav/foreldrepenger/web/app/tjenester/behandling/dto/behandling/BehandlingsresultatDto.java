package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.domene.vedtak.intern.VedtaksbrevStatus;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BehandlingsresultatDto {

    @NotNull @JsonProperty("id")
    private Long id;
    @NotNull @JsonProperty("type")
    private BehandlingResultatType type;
    @JsonProperty("avslagsarsak")
    private Avslagsårsak avslagsarsak;
    @JsonProperty("avslagsarsakFritekst")
    private String avslagsarsakFritekst;
    @JsonProperty("rettenTil")
    private RettenTil rettenTil;
    @JsonProperty("konsekvenserForYtelsen")
    private List<KonsekvensForYtelsen> konsekvenserForYtelsen;
    @JsonProperty("vedtaksbrev")
    private Vedtaksbrev vedtaksbrev;
    @NotNull @JsonProperty("vedtaksbrevStatus")
    private VedtaksbrevStatus vedtaksbrevStatus;
    @JsonProperty("overskrift")
    private String overskrift;
    @JsonProperty("fritekstbrev")
    private String fritekstbrev;
    @NotNull @JsonProperty("harRedigertVedtaksbrev")
    private boolean harRedigertVedtaksbrev;
    @JsonProperty("erRevurderingMedUendretUtfall")
    private Boolean erRevurderingMedUendretUtfall;
    @JsonProperty("skjæringstidspunkt")
    private SkjæringstidspunktDto skjæringstidspunkt;
    @JsonProperty("endretDekningsgrad")
    private boolean endretDekningsgrad;
    @JsonProperty("opphørsdato")
    private LocalDate opphørsdato;

    public BehandlingsresultatDto() {
        // trengs for deserialisering av JSON
    }

    void setId(Long id) {
        this.id = id;
    }

    void setType(BehandlingResultatType type) {
        this.type = type;
    }

    void setAvslagsarsak(Avslagsårsak avslagsarsak) {
        this.avslagsarsak = avslagsarsak;
    }

    void setAvslagsarsakFritekst(String avslagsarsakFritekst) {
        this.avslagsarsakFritekst = avslagsarsakFritekst;
    }

    public void setRettenTil(RettenTil rettenTil) {
        this.rettenTil = rettenTil;
    }

    public void setKonsekvenserForYtelsen(List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        this.konsekvenserForYtelsen = konsekvenserForYtelsen;
    }

    public void setVedtaksbrev(Vedtaksbrev vedtaksbrev) {
        this.vedtaksbrev = vedtaksbrev;
    }

    public Long getId() {
        return id;
    }

    public BehandlingResultatType getType() {
        return type;
    }

    public Avslagsårsak getAvslagsarsak() {
        return avslagsarsak;
    }

    public String getAvslagsarsakFritekst() {
        return avslagsarsakFritekst;
    }

    public RettenTil getRettenTil() {
        return rettenTil;
    }

    public List<KonsekvensForYtelsen> getKonsekvenserForYtelsen() {
        return konsekvenserForYtelsen;
    }

    public Vedtaksbrev getVedtaksbrev() {
        return vedtaksbrev;
    }

    public Boolean getErRevurderingMedUendretUtfall() {
        return Boolean.TRUE.equals(erRevurderingMedUendretUtfall);
    }

    public void setErRevurderingMedUendretUtfall(Boolean erRevurderingMedUendretUtfall) {
        this.erRevurderingMedUendretUtfall = erRevurderingMedUendretUtfall;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public void setOverskrift(String overskrift) {
        this.overskrift = overskrift;
    }

    public String getFritekstbrev() {
        return fritekstbrev;
    }

    public void setFritekstbrev(String fritekstbrev) {
        this.fritekstbrev = fritekstbrev;
    }

    public boolean isHarRedigertVedtaksbrev() {
        return harRedigertVedtaksbrev;
    }

    public void setHarRedigertVedtaksbrev(boolean harRedigertVedtaksbrev) {
        this.harRedigertVedtaksbrev = harRedigertVedtaksbrev;
    }

    public VedtaksbrevStatus getVedtaksbrevStatus() {
        return vedtaksbrevStatus;
    }

    public void setVedtaksbrevStatus(VedtaksbrevStatus vedtaksbrevStatus) {
        this.vedtaksbrevStatus = vedtaksbrevStatus;
    }

    public SkjæringstidspunktDto getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(SkjæringstidspunktDto skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public void setEndretDekningsgrad(Boolean endretDekningsgrad) {
        this.endretDekningsgrad = endretDekningsgrad;
    }

    public Boolean isEndretDekningsgrad() {
        return endretDekningsgrad;
    }

    public LocalDate getOpphørsdato() {
        return opphørsdato;
    }

    public void setOpphørsdato(LocalDate opphørsdato) {
        this.opphørsdato = opphørsdato;
    }
}
