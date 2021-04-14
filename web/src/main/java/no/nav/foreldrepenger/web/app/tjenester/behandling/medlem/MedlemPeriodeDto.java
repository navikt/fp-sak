package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.domene.medlem.api.VurderingsÅrsak;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonopplysningMedlemDto;

public class MedlemPeriodeDto {

    private LocalDate vurderingsdato;
    private Set<String> aksjonspunkter = Collections.emptySet();
    private Set<VurderingsÅrsak> årsaker = Collections.emptySet();
    private Boolean oppholdsrettVurdering;
    private Boolean erEosBorger;
    private Boolean lovligOppholdVurdering;
    private Boolean bosattVurdering;
    private MedlemskapManuellVurderingType medlemskapManuellVurderingType;
    private String begrunnelse;
    private PersonopplysningMedlemDto personopplysningBruker;
    private PersonopplysningMedlemDto personopplysningAnnenPart;

    public MedlemPeriodeDto() {
        // trengs for deserialisering av JSON
    }

    public LocalDate getVurderingsdato() {
        return vurderingsdato;
    }

    void setVurderingsdato(LocalDate vurderingsdato) {
        this.vurderingsdato = vurderingsdato;
    }

    public Set<String> getAksjonspunkter() {
        return aksjonspunkter;
    }

    void setAksjonspunkter(Set<String> aksjonspunkter) {
        this.aksjonspunkter = aksjonspunkter;
    }

    public Set<VurderingsÅrsak> getÅrsaker() {
        return årsaker;
    }

    void setÅrsaker(Set<VurderingsÅrsak> årsaker) {
        this.årsaker = årsaker;
    }

    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    void setOppholdsrettVurdering(Boolean oppholdsrettVurdering) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
    }

    public Boolean getErEosBorger() {
        return erEosBorger;
    }

    void setErEosBorger(Boolean erEosBorger) {
        this.erEosBorger = erEosBorger;
    }

    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    void setLovligOppholdVurdering(Boolean lovligOppholdVurdering) {
        this.lovligOppholdVurdering = lovligOppholdVurdering;
    }

    public Boolean getBosattVurdering() {
        return bosattVurdering;
    }

    void setBosattVurdering(Boolean bosattVurdering) {
        this.bosattVurdering = bosattVurdering;
    }

    public MedlemskapManuellVurderingType getMedlemskapManuellVurderingType() {
        return medlemskapManuellVurderingType;
    }

    void setMedlemskapManuellVurderingType(MedlemskapManuellVurderingType medlemskapManuellVurderingType) {
        this.medlemskapManuellVurderingType = medlemskapManuellVurderingType;
    }

    public PersonopplysningMedlemDto getPersonopplysningBruker() {
        return personopplysningBruker;
    }

    public void setPersonopplysningBruker(PersonopplysningMedlemDto personopplysningBruker) {
        this.personopplysningBruker = personopplysningBruker;
    }

    public PersonopplysningMedlemDto getPersonopplysningAnnenPart() {
        return personopplysningAnnenPart;
    }

    public void setPersonopplysningAnnenPart(PersonopplysningMedlemDto personopplysningAnnenPart) {
        this.personopplysningAnnenPart = personopplysningAnnenPart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (MedlemPeriodeDto) o;
        return Objects.equals(vurderingsdato, that.vurderingsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vurderingsdato);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }
}
