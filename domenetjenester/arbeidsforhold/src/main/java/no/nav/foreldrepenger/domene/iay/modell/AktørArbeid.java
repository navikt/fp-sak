package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class AktørArbeid implements IndexKey {

    private AktørId aktørId;

    @ChangeTracked
    private Set<Yrkesaktivitet> yrkesaktiviter = new LinkedHashSet<>();

    AktørArbeid() {
        // hibernate
    }

    /**
     * Deep copy ctor
     */
    AktørArbeid(AktørArbeid aktørArbeid) {
        this.aktørId = aktørArbeid.getAktørId();

        this.yrkesaktiviter = aktørArbeid.yrkesaktiviter.stream().map(Yrkesaktivitet::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getAktørId());
    }

    /**
     * Aktøren som avtalene gjelder for
     *
     * @return aktørId
     */
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    /**
     * Ufiltrert liste av yrkesaktiviteter.
     */
    public Collection<Yrkesaktivitet> hentAlleYrkesaktiviteter() {
        return Set.copyOf(yrkesaktiviter);
    }

    boolean hasValues() {
        return aktørId != null || yrkesaktiviter != null;
    }

    YrkesaktivitetBuilder getYrkesaktivitetBuilderForNøkkel(Opptjeningsnøkkel identifikator, ArbeidType arbeidType) {
        var yrkesaktivitet = yrkesaktiviter.stream()
            .filter(ya -> ya.getArbeidType().equals(arbeidType) && new Opptjeningsnøkkel(ya).equals(identifikator))
            .findFirst();
        var oppdatere = YrkesaktivitetBuilder.oppdatere(yrkesaktivitet);
        oppdatere.medArbeidType(arbeidType);
        return oppdatere;
    }

    YrkesaktivitetBuilder getYrkesaktivitetBuilderForNøkkel(Opptjeningsnøkkel identifikator, Set<ArbeidType> arbeidTyper) {
        var yrkesaktivitet = yrkesaktiviter.stream()
            .filter(ya -> arbeidTyper.contains(ya.getArbeidType()) && new Opptjeningsnøkkel(ya).equals(identifikator))
            .findFirst();
        var oppdatere = YrkesaktivitetBuilder.oppdatere(yrkesaktivitet);
        if (!oppdatere.getErOppdatering()) {
            // Defaulter til ordinert arbeidsforhold hvis saksbehandler har lagt til fra GUI
            oppdatere.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        }
        return oppdatere;
    }

    void fjernYrkesaktivitetForBuilder(YrkesaktivitetBuilder builder) {
        var yrkesaktivitetKladd = builder.getKladd();
        var arbeidType = yrkesaktivitetKladd.getArbeidType();
        if (arbeidType.erAnnenOpptjening() || ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(arbeidType)) {
            yrkesaktiviter.removeIf(ya -> ya.getArbeidType().equals(arbeidType));
        } else {
            var nøkkel = new Opptjeningsnøkkel(yrkesaktivitetKladd);
            yrkesaktiviter.removeIf(ya -> ya.getArbeidType().equals(arbeidType) && new Opptjeningsnøkkel(ya).matcher(nøkkel));
        }
    }

    YrkesaktivitetBuilder getYrkesaktivitetBuilderForType(ArbeidType type) {
        var yrkesaktivitet = yrkesaktiviter.stream().filter(ya -> ya.getArbeidType().equals(type)).findFirst();
        var oppdatere = YrkesaktivitetBuilder.oppdatere(yrkesaktivitet);
        oppdatere.medArbeidType(type);
        return oppdatere;
    }

    void leggTilYrkesaktivitet(Yrkesaktivitet yrkesaktivitet) {
        this.yrkesaktiviter.add(yrkesaktivitet);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AktørArbeid other)) {
            return false;
        }
        return Objects.equals(this.getAktørId(), other.getAktørId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + "aktørId=" + aktørId + ", yrkesaktiviteter=" + yrkesaktiviter + '>';
    }

}
