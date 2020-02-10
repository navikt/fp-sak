package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Optional;

public class VurdertLøpendeMedlemskapBuilder {
    private VurdertLøpendeMedlemskapEntitet medlemskapMal;
    private boolean oppdatering = false;

    private VurdertLøpendeMedlemskapBuilder(VurdertLøpendeMedlemskapEntitet medlemskap) {
        if (medlemskap != null) {
            this.medlemskapMal = medlemskap;
            this.oppdatering = true;
        } else {
            medlemskapMal = new VurdertLøpendeMedlemskapEntitet();
        }
    }

    VurdertLøpendeMedlemskapBuilder(Optional<VurdertLøpendeMedlemskapEntitet> vurdertMedlemskap) {
        this(vurdertMedlemskap.orElse(null));
    }

    public VurdertLøpendeMedlemskapBuilder medOppholdsrettVurdering(Boolean oppholdsrettVurdering) {
        medlemskapMal.setOppholdsrettVurdering(oppholdsrettVurdering);
        return this;
    }

    public VurdertLøpendeMedlemskapBuilder medLovligOppholdVurdering(Boolean lovligOppholdVurdering) {
        medlemskapMal.setLovligOppholdVurdering(lovligOppholdVurdering);
        return this;
    }

    public VurdertLøpendeMedlemskapBuilder medBosattVurdering(Boolean bosattVurdering) {
        medlemskapMal.setBosattVurdering(bosattVurdering);
        return this;
    }

    public VurdertLøpendeMedlemskapBuilder medErEosBorger(Boolean erEosBorger) {
        medlemskapMal.setErEøsBorger(erEosBorger);
        return this;
    }

    public VurdertLøpendeMedlemskapBuilder medBegrunnelse(String begrunnelse) {
        medlemskapMal.setBegrunnelse(begrunnelse);
        return this;
    }

    public VurdertLøpendeMedlemskapBuilder medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType manuellVurderingType) {
        medlemskapMal.setMedlemsperiodeManuellVurdering(manuellVurderingType);
        return this;
    }

    public VurdertLøpendeMedlemskapBuilder medVurderingsdato(LocalDate vurderingsdato) {
        medlemskapMal.setVuderingsdato(vurderingsdato);
        return this;
    }

    public boolean erOppdatering() {
        return oppdatering;
    }

    public VurdertLøpendeMedlemskapEntitet build() {
        return medlemskapMal;
    }
}
