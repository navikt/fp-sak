package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.es;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.skjæringstidspunkt.es.BotidCore2024;


@ApplicationScoped
public class Medlemsvilkårutleder {
    private static final Logger LOG = LoggerFactory.getLogger(Medlemsvilkårutleder.class);
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    private BotidCore2024 botidCore2024;


    Medlemsvilkårutleder() {
        // CDI
    }

    @Inject
    public Medlemsvilkårutleder(BehandlingRepositoryProvider repositoryProvider,
                                BotidCore2024 botidCore2024) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.botidCore2024 = botidCore2024;
    }

    public BehandleStegResultat opprettVilkårForBehandling(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId()).orElse(null);
        var utledetVilkår = botidCore2024.ikkeBotidskrav(familieHendelseGrunnlag) ? VilkårType.MEDLEMSKAPSVILKÅRET : VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE;
        var fjernVilkår = VilkårType.MEDLEMSKAPSVILKÅRET.equals(utledetVilkår) ? VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE : VilkårType.MEDLEMSKAPSVILKÅRET;
        opprettVilkårVedBehov(utledetVilkår, fjernVilkår, behandling, kontekst.getSkriveLås());
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void opprettVilkårVedBehov(VilkårType utledetVilkår, VilkårType fjernVilkår, Behandling behandling, BehandlingLås skriveLås) {
        // Opprett Vilkårsresultat riktig medlemsvilkår, og sett dem som ikke vurdert. Fjerne overskuddsvilkår.
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        var vilkårTyper = behandlingsresultat.map(Behandlingsresultat::getVilkårResultat).map(VilkårResultat::getVilkårTyper).orElseGet(Set::of);
        if (vilkårTyper.contains(utledetVilkår) && !vilkårTyper.contains(fjernVilkår)) {
            LOG.info("Engangsstønad Hadde allerede utledet Medlemsvilkår {}", utledetVilkår);
            return;
        }
        LOG.info("Engangsstønad setter Utledet Medlemsvilkår {} hadde {}", utledetVilkår, fjernVilkår);
        var vilkårBuilder = behandlingsresultat.map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::builderFraEksisterende)
            .orElseGet(VilkårResultat::builder);
        if (!vilkårTyper.contains(utledetVilkår)) {
            vilkårBuilder.leggTilVilkårIkkeVurdert(utledetVilkår);
        }
        if (vilkårTyper.contains(fjernVilkår)) {
            vilkårBuilder.fjernVilkår(fjernVilkår);
        }
        var vilkårResultat = vilkårBuilder.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, skriveLås);
    }
}
