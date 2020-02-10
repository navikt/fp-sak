package no.nav.foreldrepenger.mottak.hendelser.fp;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.FØDSEL_HENDELSE)
@FagsakYtelseTypeRef("FP")
public class FødselForretningshendelseHåndtererImpl implements ForretningshendelseHåndterer {

    private ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public FødselForretningshendelseHåndtererImpl(BehandlingRepositoryProvider repositoryProvider,
                                                ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.fellesHåndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
        vurderNyStartdato(åpenBehandling);
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
    }

    @Override
    public void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> køetBehandlingOpt = behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId());
        forretningshendelseHåndtererFelles.fellesHåndterKøetBehandling(fagsak, behandlingÅrsakType, køetBehandlingOpt);
    }

    private void vurderNyStartdato(Behandling behandling) {
        if (RelasjonsRolleType.MORA.equals(behandling.getFagsak().getRelasjonsRolleType())) {
            familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
                .ifPresent(fg -> fg.getGjeldendeBekreftetVersjon().flatMap(FamilieHendelseEntitet::getFødselsdato)
                    .ifPresent(nyBekreftetFødselsdato -> {
                        if (nyBekreftetFødselsdato.isBefore(getNåværendeStartdato(behandling))) {
                            settNyStartdatoOgOpprettHistorikkInnslag(behandling, nyBekreftetFødselsdato);
                        }
                    })
                );
        }
    }

    private LocalDate getNåværendeStartdato(Behandling behandling) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato)
            .or(() -> skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getSkjæringstidspunktHvisUtledet())
            .orElse(LocalDate.MAX);
    }

    private void settNyStartdatoOgOpprettHistorikkInnslag(Behandling behandling, LocalDate nyBekreftetFødselsdato) {
        final YtelseFordelingAggregat aggregat = ytelsesFordelingRepository.hentAggregat(behandling.getId());
        final Optional<AvklarteUttakDatoerEntitet> avklarteDatoer = aggregat.getAvklarteDatoer();
        final LocalDate gammelStartdato = avklarteDatoer.map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato).orElse(null);
        final AvklarteUttakDatoerEntitet entitet = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer).medFørsteUttaksdato(nyBekreftetFødselsdato).build();
        ytelsesFordelingRepository.lagre(behandling.getId(), entitet);
        historikkinnslagTjeneste.opprettHistorikkinnslagForEndretStartdatoEtterFødselshendelse(behandling, gammelStartdato, nyBekreftetFødselsdato);
    }
}

