package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingHistorikk;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.domene.person.tps.TpsFamilieTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@ProsessTask(AutomatiskEtterkontrollTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskEtterkontrollTask extends FagsakProsessTask {
    public static final String TASKTYPE = "behandlingsprosess.etterkontroll";
    private static final Logger log = LoggerFactory.getLogger(AutomatiskEtterkontrollTask.class);
    private TpsFamilieTjeneste tpsFamilieTjeneste;
    private EtterkontrollTjeneste etterkontrollTjeneste;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private Period tpsRegistreringsTidsrom;
    private EtterkontrollRepository etterkontrollRepository;

    private RevurderingHistorikk revurderingHistorikk;

    private BehandlingVedtakRepository behandlingVedtakRepository;


    AutomatiskEtterkontrollTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskEtterkontrollTask(BehandlingRepositoryProvider repositoryProvider,// NOSONAR
                                       EtterkontrollRepository etterkontrollRepository,
                                       HistorikkRepository historikkRepository,
                                       FamilieHendelseTjeneste familieHendelseTjeneste,
                                       TpsFamilieTjeneste tpsFamilieTjeneste,
                                       ProsessTaskRepository prosessTaskRepository,
                                       @KonfigVerdi(value = "etterkontroll.tpsregistrering.periode", defaultVerdi = "P11W") Period tpsRegistreringsTidsrom,
                                       BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                       EtterkontrollTjeneste etterkontrollTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.tpsFamilieTjeneste = tpsFamilieTjeneste;
        this.etterkontrollTjeneste = etterkontrollTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.revurderingHistorikk = new RevurderingHistorikk(historikkRepository);
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.tpsRegistreringsTidsrom = tpsRegistreringsTidsrom;
        this.etterkontrollRepository = etterkontrollRepository;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        log.info("Etterkontrollerer fagsak med fagsakId = {}", fagsakId);

        etterkontrollRepository.avflaggDersomEksisterer(fagsakId, KontrollType.MANGLENDE_FØDSEL);

        Behandling behandlingForRevurdering = behandlingRepository.hentBehandling(behandlingId);

        if (behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId)) {
            opprettTaskForÅVurdereKonsekvens(fagsakId, behandlingForRevurdering.getBehandlendeEnhet());
            return;
        }

        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandlingForRevurdering.getFagsak().getYtelseType()).orElseThrow();

        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieHendelseTjeneste.finnAggregat(behandlingForRevurdering.getId()).orElse(null);

        BehandlingÅrsakType revurderingsÅrsak = BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN;

        if (familieHendelseGrunnlag != null) {
            var intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandlingForRevurdering));
            List<FødtBarnInfo> barnFødtIPeriode = tpsFamilieTjeneste.getFødslerRelatertTilBehandling(behandlingForRevurdering.getAktørId(), intervaller);
            if (!barnFødtIPeriode.isEmpty()) {
                revurderingHistorikk.opprettHistorikkinnslagForFødsler(behandlingForRevurdering, barnFødtIPeriode);
            }
            int antallBarnTps = barnFødtIPeriode.size();

            Optional<BehandlingÅrsakType> utledetÅrsak = utledRevurderingsÅrsak(behandlingForRevurdering, familieHendelseGrunnlag, antallBarnTps);
            if (utledetÅrsak.isEmpty()) {
                if (skalReberegneES(behandlingForRevurdering, barnFødtIPeriode)) {
                    utledetÅrsak = Optional.of(BehandlingÅrsakType.RE_SATS_REGULERING);
                } else {
                    return;
                }
            }
            revurderingsÅrsak = utledetÅrsak.get();
            if (BehandlingÅrsakType.RE_MANGLER_FØDSEL.equals(revurderingsÅrsak) && tpsFamilieTjeneste.harBrukerDnr(behandlingForRevurdering.getAktørId())) {
                // Disse har ikke registrert barn-relasjon i TPS - ikke send brev
                revurderingsÅrsak = BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE;
            }
        }

        Behandling opprettetRevurdering = opprettRevurdering(behandlingForRevurdering.getFagsak(), revurderingTjeneste, revurderingsÅrsak);
        if (opprettetRevurdering != null) {
            log.info("Etterkontroll har opprettet revurdering med id {} på fagsak med id = {} for behandling med id {}",opprettetRevurdering.getId(), fagsakId, behandlingForRevurdering.getId());
            etterkontrollTjeneste.utfør(behandlingForRevurdering, opprettetRevurdering);
        }
    }

    private Optional<BehandlingÅrsakType> utledRevurderingsÅrsak(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag, int antallBarnTps) {
        BehandlingÅrsakType revurderingsÅrsak = BehandlingÅrsakType.UDEFINERT;

        int antallBarnSakBekreftet = finnAntallBekreftet(behandling, grunnlag);

        if (antallBarnTps == 0 && finnAntallOverstyrtManglendeFødsel(grunnlag) > 0) {
            return Optional.empty();
        }
        if (antallBarnSakBekreftet > 0 && antallBarnSakBekreftet == antallBarnTps) {
            return Optional.empty();
        }

        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsak().getYtelseType())) {
            revurderingsÅrsak = utledRevurderingsÅrsakES(behandling, grunnlag, antallBarnTps);
        } else if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsak().getYtelseType())) {
            revurderingsÅrsak = utledRevurderingsÅrsakFP(antallBarnTps, antallBarnSakBekreftet);
        }
        return Optional.of(revurderingsÅrsak);
    }

    private BehandlingÅrsakType utledRevurderingsÅrsakFP(int antallBarnTps, int antallBarnSakBekreftet) {
        if (antallBarnTps == 0) {
            return BehandlingÅrsakType.RE_MANGLER_FØDSEL;
        }
        return antallBarnSakBekreftet == 0 ? BehandlingÅrsakType.RE_HENDELSE_FØDSEL : BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN;
    }

    private BehandlingÅrsakType utledRevurderingsÅrsakES(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag, int antallBarnTps) {
        if (antallBarnTps > 0) {
            return BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN;
        }
        // Kun relevant for ES. FP kan ikke behandles før minimum(fødsel, TERMIN-4U)
        BehandlingÅrsakType revurderingÅrsak = BehandlingÅrsakType.RE_MANGLER_FØDSEL;
        Optional<LocalDate> termindato = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
        if (termindato.isPresent()) {
            LocalDate tidligsteTpsRegistreringsDato = termindato.get().minus(tpsRegistreringsTidsrom);
            var vedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());
            LocalDate vedtaksDato = vedtak.getVedtaksdato();
            if (vedtaksDato.isBefore(tidligsteTpsRegistreringsDato)) {
                revurderingÅrsak = BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE;
            }
        }
        return revurderingÅrsak;
    }

    private boolean skalReberegneES(Behandling behandling, List<FødtBarnInfo> fødteBarn) {
        var fødselsdato = fødteBarn.stream().map(FødtBarnInfo::getFødselsdato).max(Comparator.naturalOrder()).orElse(null);
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) && fødselsdato != null) {
            return etterkontrollTjeneste.skalReberegneES(behandling, fødselsdato);
        }
        return false;
    }

    private int finnAntallBekreftet(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag) {
        int antallBarn = grunnlag.getGjeldendeBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0); // Inkluderer termin/overstyrt
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsak().getYtelseType())) {
            // FP: Revurdering dersom mangler i TPS (SMF), antallDiff(AAB). Dessuten revurdering u/brev hvis det mangler bekreftet eller os/fødsel.
            antallBarn = grunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0);
            if (antallBarn == 0) {
                antallBarn = finnAntallOverstyrtManglendeFødsel(grunnlag);
            }
        }
        return antallBarn;
    }

    private int finnAntallOverstyrtManglendeFødsel(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getOverstyrtVersjon().filter(fh -> FamilieHendelseType.FØDSEL.equals(fh.getType())).map(FamilieHendelseEntitet::getAntallBarn).orElse(0);
    }

    private Behandling opprettRevurdering(Fagsak fagsakForRevurdering, RevurderingTjeneste revurderingTjeneste, BehandlingÅrsakType behandlingÅrsakType) {
        OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsakForRevurdering);

        return revurderingTjeneste.opprettAutomatiskRevurdering(fagsakForRevurdering, behandlingÅrsakType, enhet);
    }

    private void opprettTaskForÅVurdereKonsekvens(Long fagsakId, String behandlendeEnhetsId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, "Kontroller manglende fødselsregistrering");
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_NORM);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
