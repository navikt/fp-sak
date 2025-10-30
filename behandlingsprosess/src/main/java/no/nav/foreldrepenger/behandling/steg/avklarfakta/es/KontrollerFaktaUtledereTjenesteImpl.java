package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaUtledere;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.VilkårUtlederFeil;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel.AksjonspunktUtlederForEngangsstønadFødsel;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.sammebarn.AksjonspunktUtlederForTidligereMottattYtelse;

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

        var utlederHolder = new AksjonspunktUtlederHolder();

        var behandlingId = ref.behandlingId();
        var hendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (hendelseGrunnlag.isEmpty()) {
            throw VilkårUtlederFeil.behandlingsmotivKanIkkeUtledes(behandlingId);
        }

        var familieHendelseType = hendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getType)
                .orElseThrow(() -> new IllegalStateException("Utvikler feil: Hendelse uten type"));

        if (FamilieHendelseType.FØDSEL.equals(familieHendelseType) || FamilieHendelseType.TERMIN.equals(familieHendelseType)) {
            utlederHolder.leggTil(AksjonspunktUtlederForEngangsstønadFødsel.class);
            utlederHolder.leggTil(AksjonspunktUtlederForTidligereMottattYtelse.class);
        }

        return utlederHolder.getUtledere();
    }
}
