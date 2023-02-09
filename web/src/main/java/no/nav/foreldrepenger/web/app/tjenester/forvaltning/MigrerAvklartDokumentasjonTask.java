package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.ALENEOMSORG_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.ALENEOMSORG_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.BARE_SØKER_RETT_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.BARE_SØKER_RETT_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.HV_OVELSE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.HV_OVELSE_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_ANNEN_FORELDER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_BARN_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_BARN_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.INNLEGGELSE_SØKER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_IKKE_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.MORS_AKTIVITET_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.NAV_TILTAK_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.NAV_TILTAK_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_ANNEN_FORELDER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_SØKER_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.SYKDOM_SØKER_IKKE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.UttakDokumentasjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.DokumentasjonVurderingBehov;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskDataBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@Dependent
@ProsessTask(value = "migrering.yf.dokumentasjon", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MigrerAvklartDokumentasjonTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MigrerAvklartDokumentasjonTask.class);

    private InformasjonssakRepository informasjonssakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private VurderUttakDokumentasjonAksjonspunktUtleder utleder;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public MigrerAvklartDokumentasjonTask(InformasjonssakRepository informasjonssakRepository,
                                          YtelsesFordelingRepository ytelsesFordelingRepository,
                                          BehandlingRepository behandlingRepository,
                                          TotrinnTjeneste totrinnTjeneste,
                                          ProsessTaskTjeneste taskTjeneste,
                                          VurderUttakDokumentasjonAksjonspunktUtleder utleder,
                                          UttakInputTjeneste uttakInputTjeneste) {
        this.informasjonssakRepository = informasjonssakRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.utleder = utleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    MigrerAvklartDokumentasjonTask() {
        // for CDI proxy
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        var behandlinger = informasjonssakRepository.finnYtelsesfordelingForMigreringAvklartDok();
        if (behandlinger.isEmpty()) {
            LOG.info("Migrering uttak - ingen flere kandidater for migrering");
            return;
        }
        behandlinger.forEach(this::migrer);

        var task = ProsessTaskDataBuilder.forProsessTask(MigrerAvklartDokumentasjonTask.class)
            .medCallId(callId)
            .medPrioritet(100)
            .build();
//        taskTjeneste.lagre(task);
    }

    private void migrer(Long behandlingId) {
        LOG.info("Migrering uttak - kjøres for behandling {}", behandlingId);
        // sikre at vi ikke går i beina på noe annet som skjer
        var behandlingLås = behandlingRepository.taSkriveLås(behandlingId);
        var yfa = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId).orElseThrow();
        var eksisterende = yfa.getGjeldendeFordeling();
        var erAnnenForelderInformert = eksisterende.getErAnnenForelderInformert();
        var ønskerJustertVedFødsel = eksisterende.ønskerJustertVedFødsel();

        var input = uttakInputTjeneste.lagInput(behandlingId);
        var migrertePerioder = eksisterende.getPerioder().stream().flatMap(p -> migrer(p, yfa, input)).toList();
        var migreringFørerTilEndring = !migrertePerioder.equals(eksisterende.getPerioder());
        if (!migreringFørerTilEndring) {
            LOG.info("Migrering uttak - Finner ingen endringer {} {} {}", behandlingId, eksisterende.getPerioder(), migrertePerioder);
        }
        var migrertFordeling = new OppgittFordelingEntitet(migrertePerioder, erAnnenForelderInformert, ønskerJustertVedFødsel);
        var builder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .migrertDokumentasjonsPerioder(true);
        if (migreringFørerTilEndring) {
            if (yfa.getOverstyrtFordeling().isPresent()) {
                builder = builder.medOverstyrtFordeling(migrertFordeling);
            } else if (yfa.getJustertFordeling().isPresent()) {
                builder = builder.medJustertFordeling(migrertFordeling);
            } else {
                builder = builder.medOppgittFordeling(migrertFordeling);
            }
        }
        ytelsesFordelingRepository.lagre(behandlingId, builder.build());
        oppdaterToTrinnGrunnlag(behandlingId);
    }

    private void oppdaterToTrinnGrunnlag(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var totrinnresultatgrunnlag = totrinnTjeneste.hentTotrinngrunnlagHvisEksisterer(behandlingId);
        totrinnresultatgrunnlag.ifPresent(tg -> {
            var nyYfGrunnlagId = ytelsesFordelingRepository.hentIdPåAktivYtelsesFordeling(behandling.getId()).orElse(null);
            var nyttGrunnlag = new Totrinnresultatgrunnlag(behandling, nyYfGrunnlagId,
                tg.getUttakResultatEntitetId().orElse(null), tg.getBeregningsgrunnlagId().orElse(null), tg.getGrunnlagUuid().orElse(null));
            totrinnTjeneste.lagreNyttTotrinnresultat(nyttGrunnlag);
        });
    }

    private Stream<OppgittPeriodeEntitet> migrer(OppgittPeriodeEntitet periode, YtelseFordelingAggregat yfa, UttakInput input) {
        var behov = utleder.dokumentasjonVurderingBehov(periode, input);
        if (!behov.måVurderes()) {
            return Stream.of(OppgittPeriodeBuilder.fraEksisterende(periode).build());
        }
        return avklarDokForPeriode(periode, behov, yfa, input.getBehandlingReferanse());
    }

    private Stream<OppgittPeriodeEntitet> avklarDokForPeriode(OppgittPeriodeEntitet periode,
                                                              DokumentasjonVurderingBehov vurderingBehov,
                                                              YtelseFordelingAggregat ytelseFordelingAggregat,
                                                              BehandlingReferanse ref) {
        var dokPerioder = ytelseFordelingAggregat.getPerioderUttakDokumentasjon().map(d -> d.getPerioder()).orElse(List.of());
        var dokTimeline = new LocalDateTimeline<>(
            dokPerioder.stream().map(dp -> new LocalDateSegment<>(dp.getPeriode().getFomDato(), dp.getPeriode().getTomDato(), dp)).toList());
        var aktKravPerioder = ytelseFordelingAggregat.getGjeldendeAktivitetskravPerioder().map(d -> d.getPerioder()).orElse(List.of());
        return switch (vurderingBehov.behov().type()) {
            case UTSETTELSE -> avklarUtsettelse(periode, vurderingBehov.behov().årsak(), dokTimeline);
            case OVERFØRING -> avklarOverføring(periode, vurderingBehov.behov().årsak(), dokTimeline);
            case UTTAK -> Stream.of(avklarUttak(periode, vurderingBehov.behov().årsak(), aktKravPerioder, ref));
        };
    }

    private OppgittPeriodeEntitet avklarUttak(OppgittPeriodeEntitet periode,
                                              DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                              List<AktivitetskravPeriodeEntitet> aktKravTimeline,
                                              BehandlingReferanse ref) {
        var avklartDokVurdering = switch (årsak) {
            case AKTIVITETSKRAV_ARBEID, AKTIVITETSKRAV_IKKE_OPPGITT, AKTIVITETSKRAV_ARBEID_OG_UTDANNING, AKTIVITETSKRAV_INNLAGT,
                AKTIVITETSKRAV_TRENGER_HJELP, AKTIVITETSKRAV_INTROPROG, AKTIVITETSKRAV_KVALPROG, AKTIVITETSKRAV_UTDANNING -> avklarAktivitetskrav(periode, aktKravTimeline, ref);
            case TIDLIG_OPPSTART_FAR -> avklarTidligOppstart(periode);
            default -> throw new IllegalStateException("Unexpected value: " + årsak);
        };
        return OppgittPeriodeBuilder.fraEksisterende(periode)
            .medDokumentasjonVurdering(avklartDokVurdering.vurdering)
            .medBegrunnelse(avklartDokVurdering.begrunnelse)
            .build();
    }

    private AvklaringMedBegrunnelse avklarTidligOppstart(OppgittPeriodeEntitet periode) {
        var vurdering = tidligOppstartFedrekvoteAvklart(periode) ? TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT : TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT;
        return new AvklaringMedBegrunnelse(vurdering, periode.getBegrunnelse().orElse(null));
    }

    private record AvklaringMedBegrunnelse(DokumentasjonVurdering vurdering, String begrunnelse) {

    }

    private AvklaringMedBegrunnelse avklarAktivitetskrav(OppgittPeriodeEntitet periode, List<AktivitetskravPeriodeEntitet> aktKravTimeline, BehandlingReferanse referanse) {
        for (var avklaring : aktKravTimeline) {
            if (periode.getTidsperiode().erOmsluttetAv(avklaring.getTidsperiode())) {
                var begrunnelse = periode.getBegrunnelse().map(b -> b + " - " + avklaring.getBegrunnelse()).orElse(avklaring.getBegrunnelse());
                return new AvklaringMedBegrunnelse(utledDokumentasjonVurdering(avklaring), begrunnelse);
            }
        }
        LOG.info("Migrering uttak - Mangler aktivitetskrav avklaring for periode {}, behandling {}", periode, referanse);
        return new AvklaringMedBegrunnelse(null, periode.getBegrunnelse().orElse(null));
    }

    private Stream<OppgittPeriodeEntitet> avklarUtsettelse(OppgittPeriodeEntitet periode,
                                                           DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                                           LocalDateTimeline<PeriodeUttakDokumentasjonEntitet> dokTimeline) {

        var tilhørendeAvklaring = switch (årsak) {
            case INNLEGGELSE_SØKER -> new TilhørendeAvklaring(UttakDokumentasjonType.INNLAGT_SØKER, INNLEGGELSE_SØKER_GODKJENT, INNLEGGELSE_SØKER_IKKE_GODKJENT);
            case INNLEGGELSE_BARN -> new TilhørendeAvklaring(UttakDokumentasjonType.INNLAGT_BARN, INNLEGGELSE_BARN_GODKJENT, INNLEGGELSE_BARN_IKKE_GODKJENT);
            case HV_ØVELSE -> new TilhørendeAvklaring(UttakDokumentasjonType.HV_OVELSE, HV_OVELSE_GODKJENT, HV_OVELSE_IKKE_GODKJENT);
            case NAV_TILTAK -> new TilhørendeAvklaring(UttakDokumentasjonType.NAV_TILTAK, NAV_TILTAK_GODKJENT, NAV_TILTAK_IKKE_GODKJENT);
            case SYKDOM_SØKER -> new TilhørendeAvklaring(UttakDokumentasjonType.SYK_SØKER, SYKDOM_SØKER_GODKJENT, SYKDOM_SØKER_IKKE_GODKJENT);
            //TODO aktivitetskrav bfhr fri utsettelse
            default -> throw new IllegalStateException("Unexpected value: " + årsak);
        };

        return avklar(periode, dokTimeline, tilhørendeAvklaring);
    }

    private Stream<OppgittPeriodeEntitet> avklarOverføring(OppgittPeriodeEntitet periode,
                                                           DokumentasjonVurderingBehov.Behov.Årsak årsak,
                                                           LocalDateTimeline<PeriodeUttakDokumentasjonEntitet> dokTimeline) {

        var tilhørendeAvklaring = switch (årsak) {
            case INNLEGGELSE_ANNEN_FORELDER -> new TilhørendeAvklaring(UttakDokumentasjonType.INSTITUSJONSOPPHOLD_ANNEN_FORELDRE, INNLEGGELSE_ANNEN_FORELDER_GODKJENT, INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT);
            case SYKDOM_ANNEN_FORELDER -> new TilhørendeAvklaring(UttakDokumentasjonType.SYKDOM_ANNEN_FORELDER, SYKDOM_ANNEN_FORELDER_GODKJENT, SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT);
            case BARE_SØKER_RETT -> new TilhørendeAvklaring(UttakDokumentasjonType.IKKE_RETT_ANNEN_FORELDER, BARE_SØKER_RETT_GODKJENT, BARE_SØKER_RETT_IKKE_GODKJENT);
            case ALENEOMSORG -> new TilhørendeAvklaring(UttakDokumentasjonType.ALENEOMSORG_OVERFØRING, ALENEOMSORG_GODKJENT, ALENEOMSORG_IKKE_GODKJENT);
            default -> throw new IllegalStateException("Unexpected value: " + årsak);
        };

        return avklar(periode, dokTimeline, tilhørendeAvklaring);
    }

    private static Stream<OppgittPeriodeEntitet> avklar(OppgittPeriodeEntitet periode,
                                                                                LocalDateTimeline<PeriodeUttakDokumentasjonEntitet> dokTimeline,
                                                                                TilhørendeAvklaring tilhørendeAvklaring) {
        var filtertDokTimeline =  new LocalDateTimeline<>(
            dokTimeline.stream().filter(s -> s.getValue().getDokumentasjonType() == tilhørendeAvklaring.gammelAvklaring).toList());
        var opTimeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getFom(), periode.getTom(), periode)));
        return opTimeline.combine(filtertDokTimeline, (interval, op, dok) -> {
            var dokumentasjonVurdering = dok == null ? tilhørendeAvklaring.negativ : tilhørendeAvklaring.positiv;
            var nyOp = OppgittPeriodeBuilder.fraEksisterende(op.getValue())
                .medPeriode(interval.getFomDato(), interval.getTomDato())
                .medDokumentasjonVurdering(dokumentasjonVurdering)
                .build();
            return new LocalDateSegment<>(interval, nyOp);
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN).toSegments().stream().map(s -> s.getValue());
    }

    private boolean tidligOppstartFedrekvoteAvklart(OppgittPeriodeEntitet op) {
        return op.getPeriodeType().equals(UttakPeriodeType.FEDREKVOTE)
            && Set.of(UttakPeriodeVurderingType.PERIODE_OK, UttakPeriodeVurderingType.PERIODE_OK_ENDRET).contains(op.getPeriodeVurderingType());
    }

    private DokumentasjonVurdering utledDokumentasjonVurdering(AktivitetskravPeriodeEntitet aktKravPeriode) {
        return switch (aktKravPeriode.getAvklaring()) {
            case I_AKTIVITET -> MORS_AKTIVITET_GODKJENT;
            case IKKE_I_AKTIVITET_IKKE_DOKUMENTERT -> MORS_AKTIVITET_IKKE_DOKUMENTERT;
            case IKKE_I_AKTIVITET_DOKUMENTERT -> MORS_AKTIVITET_IKKE_GODKJENT;
        };
    }

    private record TilhørendeAvklaring(UttakDokumentasjonType gammelAvklaring, DokumentasjonVurdering positiv, DokumentasjonVurdering negativ) {}

}
