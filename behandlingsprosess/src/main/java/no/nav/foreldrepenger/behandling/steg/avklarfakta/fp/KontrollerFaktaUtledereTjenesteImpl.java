package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import java.util.List;

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
import no.nav.foreldrepenger.domene.medlem.kontrollerfakta.AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden;
import no.nav.foreldrepenger.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskapSkjæringstidspunkt;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.adopsjon.AksjonspunktUtlederForForeldrepengerAdopsjon;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel.AksjonspunktUtlederForForeldrepengerFødsel;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.omsorg.AksjonspunktUtlederForForeldreansvar;

@ApplicationScoped
class KontrollerFaktaUtledereTjenesteImpl implements KontrollerFaktaUtledere {

    private FamilieHendelseRepository familieHendelseRepository;

    KontrollerFaktaUtledereTjenesteImpl() {
        // for CDI
    }

    @Inject
    KontrollerFaktaUtledereTjenesteImpl(FamilieHendelseRepository familieHendelseRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
    }

    // Legg til aksjonspunktutledere som er felles for Førstegangsbehandling og
    // Revurdering
    protected List<AksjonspunktUtleder> leggTilFellesutledere(BehandlingReferanse ref) {
        final var utlederHolder = new AksjonspunktUtlederHolder();

        var behandlingId = ref.getBehandlingId();
        final var hendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (hendelseGrunnlag.isEmpty()) {
            throw VilkårUtlederFeil.behandlingsmotivKanIkkeUtledes(behandlingId);
        }

        var familieHendelseType = hendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getType)
                .orElseThrow(() -> new IllegalStateException("Utvikler feil: Hendelse uten type"));

        // Legger til utledere som alltid skal kjøres
        leggTilStandardUtledere(utlederHolder);

        if (FamilieHendelseType.FØDSEL.equals(familieHendelseType) || FamilieHendelseType.TERMIN.equals(familieHendelseType)) {
            utlederHolder.leggTil(AksjonspunktUtlederForForeldrepengerFødsel.class);
        }

        if (FamilieHendelseType.ADOPSJON.equals(familieHendelseType)) {
            utlederHolder.leggTil(AksjonspunktUtlederForForeldrepengerAdopsjon.class);
        }

        if (FamilieHendelseType.OMSORG.equals(familieHendelseType)) {
            utlederHolder.leggTil(AksjonspunktUtlederForForeldreansvar.class);
        }

        return utlederHolder.getUtledere();
    }

    @Override
    public List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref) {
        return leggTilFellesutledere(ref);
    }

    private void leggTilStandardUtledere(AksjonspunktUtlederHolder utlederHolder) {
        utlederHolder.leggTil(AksjonspunktutlederForMedlemskapSkjæringstidspunkt.class)
                .leggTil(AksjonspunktUtlederForTilleggsopplysninger.class)
                .leggTil(AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden.class)
                .leggTil(AksjonspunktUtlederForTidligereMottattYtelse.class);
    }
}
