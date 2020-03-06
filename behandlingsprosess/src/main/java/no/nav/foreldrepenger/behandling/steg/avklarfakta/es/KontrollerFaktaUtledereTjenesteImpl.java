package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.AksjonspunktUtlederForTilleggsopplysninger;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaUtledere;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.VilkårUtlederFeil;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskapSkjæringstidspunkt;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.adopsjon.AksjonspunktUtlederForEngangsstønadAdopsjon;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel.AksjonspunktUtlederForEngangsstønadFødsel;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.omsorgsovertakelse.AksjonspunktUtlederForOmsorgsovertakelse;

@ApplicationScoped
class KontrollerFaktaUtledereTjenesteImpl implements KontrollerFaktaUtledere {

    private FamilieHendelseRepository familieHendelseRepository;

    KontrollerFaktaUtledereTjenesteImpl() {
        // CDI
    }

    @Inject
    KontrollerFaktaUtledereTjenesteImpl(FamilieHendelseRepository familieHendelseRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
    }

    @Override
    public List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref) {

        final AksjonspunktUtlederHolder utlederHolder = new AksjonspunktUtlederHolder();

        Long behandlingId = ref.getBehandlingId();
        final Optional<FamilieHendelseGrunnlagEntitet> hendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (hendelseGrunnlag.isEmpty()) {
            throw VilkårUtlederFeil.FEILFACTORY.behandlingsmotivKanIkkeUtledes(behandlingId).toException();
        }

        FamilieHendelseType familieHendelseType = hendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType)
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Hendelse uten type"));

        // Legger til utledere som alltid skal kjøres
        leggTilStandardUtledere(utlederHolder);

        if (FamilieHendelseType.FØDSEL.equals(familieHendelseType) || FamilieHendelseType.TERMIN.equals(familieHendelseType)) {
            utlederHolder.leggTil(AksjonspunktUtlederForEngangsstønadFødsel.class);
        }

        if (FamilieHendelseType.ADOPSJON.equals(familieHendelseType)) {
            utlederHolder.leggTil(AksjonspunktUtlederForEngangsstønadAdopsjon.class);
        }

        if (FamilieHendelseType.OMSORG.equals(familieHendelseType)) {
            utlederHolder.leggTil(AksjonspunktUtlederForOmsorgsovertakelse.class);
        }

        return utlederHolder.getUtledere();
    }

    private void leggTilStandardUtledere(AksjonspunktUtlederHolder utlederHolder) {
        utlederHolder.leggTil(AksjonspunktUtlederForTilleggsopplysninger.class)
            .leggTil(AksjonspunktUtlederForTidligereMottattYtelse.class)
            .leggTil(AksjonspunktutlederForMedlemskapSkjæringstidspunkt.class);
    }
}
