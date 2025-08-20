package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class FpInntektsmeldingTjeneste {
    private static final String GRUPPE_ID = "FPIM_TASK_%s";
    private FpinntektsmeldingKlient klient;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private HistorikkinnslagRepository historikkRepo;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;

    private static final Logger LOG = LoggerFactory.getLogger(FpInntektsmeldingTjeneste.class);

    public FpInntektsmeldingTjeneste() {
        // CDI
    }

    @Inject
    public FpInntektsmeldingTjeneste(FpinntektsmeldingKlient klient,
                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     HistorikkinnslagRepository historikkRepo,
                                     ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                     InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste) {
        this.klient = klient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkRepo = historikkRepo;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
    }

    public void lagTaskForespørAlleInntektsmeldinger(BehandlingReferanse ref) {
        lagTask(ref, null);
    }

    public void lagTaskForespørBestemtInntektsmelding(BehandlingReferanse ref, String orgnummer) {
        lagTask(ref, orgnummer);
    }

    private void lagTask(BehandlingReferanse ref, String orgnummer) {
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(FpinntektsmeldingTask.class));
        taskdata.setBehandling(ref.saksnummer().getVerdi(), ref.fagsakId(), ref.behandlingId());
        if (orgnummer != null) {
            taskdata.setProperty(FpinntektsmeldingTask.ORGNUMMER, orgnummer);
        }
        var gruppeId = String.format(GRUPPE_ID, ref.saksnummer().getVerdi());
        taskdata.setGruppe(gruppeId);
        taskdata.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        prosessTaskTjeneste.lagre(taskdata);
    }

    public void overstyrInntektsmelding(Inntektsmelding inntektsmeldingSomSkalOverstyres,
                                        Optional<Long> refusjonPrMndFraStart,
                                        Optional<LocalDate> overstyrtOpphørFom,
                                        Map<LocalDate, Beløp> overstyrteRefusjonsendringer,
                                        Optional<LocalDate> overstyrtStartdatoPermisjon,
                                        String saksbehandlerIdent,
                                        BehandlingReferanse ref) {
        var refusjonOpphørsdato = overstyrtOpphørFom.orElseGet(() -> Objects.requireNonNull(inntektsmeldingSomSkalOverstyres.getRefusjonOpphører()));
        var ytelse = ref.fagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) ? OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER : OverstyrInntektsmeldingRequest.YtelseType.SVANGERSKAPSPENGER;
        var startdato = overstyrtStartdatoPermisjon
            .orElseGet(() -> inntektsmeldingSomSkalOverstyres.getStartDatoPermisjon()
            .orElseGet(() -> skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId()).getUtledetSkjæringstidspunkt()));
        // Hvis refusjon er endret fra start brukes denne, ellers brukes gammelt refusjonsbeløp
        var refusjon = refusjonPrMndFraStart.map(BigDecimal::valueOf)
            .orElseGet(() -> Optional.ofNullable(inntektsmeldingSomSkalOverstyres.getRefusjonBeløpPerMnd())
                .map(Beløp::getVerdi).orElse(null));
        var arbeidsgiver = new OverstyrInntektsmeldingRequest.ArbeidsgiverDto(
            inntektsmeldingSomSkalOverstyres.getArbeidsgiver().getIdentifikator());
        var aktørId = new OverstyrInntektsmeldingRequest.AktørIdDto(ref.aktørId().getId());

        // Vi skal modifisere mappet, så lager et nytt her for å ikke endre input
        Map<LocalDate, Beløp> refusjonsendringer = new TreeMap<>(overstyrteRefusjonsendringer);
        inntektsmeldingSomSkalOverstyres.getEndringerRefusjon().forEach(r -> {
            if (!refusjonsendringer.containsKey(r.getFom())) {
                refusjonsendringer.put(r.getFom(), r.getRefusjonsbeløp());
            }
        });
        var endringerIRefusjon = mapRefusjonsendringer(refusjonsendringer, refusjonOpphørsdato);
        var naturalytelser = mapNaturalytelser(inntektsmeldingSomSkalOverstyres.getNaturalYtelser());
        var request = new OverstyrInntektsmeldingRequest(aktørId, arbeidsgiver, startdato, ytelse,
            inntektsmeldingSomSkalOverstyres.getInntektBeløp().getVerdi(), refusjon, endringerIRefusjon, naturalytelser, saksbehandlerIdent, new SaksnummerDto(ref.saksnummer().getVerdi()));
        klient.overstyrInntektsmelding(request);
    }

    private List<OverstyrInntektsmeldingRequest.BortfaltNaturalytelseRequestDto> mapNaturalytelser(List<NaturalYtelse> naturalYtelser) {
        return naturalYtelser.stream()
            .map(n -> new OverstyrInntektsmeldingRequest.BortfaltNaturalytelseRequestDto(n.getPeriode().getFomDato(), n.getPeriode().getTomDato(), n.getType(), n.getBeloepPerMnd().getVerdi()))
            .toList();
    }

    private List<OverstyrInntektsmeldingRequest.RefusjonendringRequestDto> mapRefusjonsendringer(Map<LocalDate, Beløp> endringerRefusjon, LocalDate overstyrtOpphørFom) {
        // Endringer etter opphørsdato er ikke relevant
        var endringer = endringerRefusjon.entrySet().stream()
            .filter(e -> e.getKey().isBefore(overstyrtOpphørFom))
            .map(e -> new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(e.getKey(), e.getValue().getVerdi()))
            .collect(Collectors.toList());
        // Setter opphør
        if (!overstyrtOpphørFom.equals(Tid.TIDENES_ENDE)) {
            endringer.add(new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(overstyrtOpphørFom, BigDecimal.ZERO));
        }
        return endringer;
    }

    public void lagForespørsel(BehandlingReferanse ref, Skjæringstidspunkt stp, Arbeidsgiver arbeidsgiver) {
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var førsteUttaksdato = stp.getFørsteUttaksdato();
        var agDto = new OrganisasjonsnummerDto(arbeidsgiver.getOrgnr());

        var request = new OpprettForespørselRequest(new OpprettForespørselRequest.AktørIdDto(ref.aktørId().getId()), null, skjæringstidspunkt,
            mapYtelsetype(ref.fagsakYtelseType()), new SaksnummerDto(ref.saksnummer().getVerdi()), førsteUttaksdato,
            List.of(agDto));

        sendRequest(ref, request);
    }

    public void lagForespørsel(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var arbeidsgivereViManglerInntektsmeldingFra = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp)
            .keySet()
            .stream()
            .filter(arbeidsgiver -> OrganisasjonsNummerValidator.erGyldig(arbeidsgiver.getOrgnr()))
            .map(arbeidsgiver -> new OrganisasjonsnummerDto(arbeidsgiver.getOrgnr()))
            .toList();
        if (arbeidsgivereViManglerInntektsmeldingFra.isEmpty()) {
            LOG.info("FpInntektsmeldingTjeneste:lagForespørsel: Ingen inntektsmeldinger mangler for sak {} og behandlingId {}", ref.saksnummer(),
                ref.behandlingId());
            return;
        }
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var førsteUttaksdato = stp.getFørsteUttaksdato();

        var request = new OpprettForespørselRequest(new OpprettForespørselRequest.AktørIdDto(ref.aktørId().getId()), null, skjæringstidspunkt,
            mapYtelsetype(ref.fagsakYtelseType()), new SaksnummerDto(ref.saksnummer().getVerdi()), førsteUttaksdato,
            arbeidsgivereViManglerInntektsmeldingFra);

        sendRequest(ref, request);
    }

    private void sendRequest(BehandlingReferanse ref,
                             OpprettForespørselRequest request) {
        LOG.info(
            "Sender kall til fpinntektsmelding om å opprette forespørsel for saksnummer {} med skjæringstidspunkt {} for følgende organisasjonsnumre: {}",
            ref.saksnummer(), request.skjæringstidspunkt(), request.organisasjonsnumre());

        var opprettForespørselResponseNy = klient.opprettForespørsel(request);

        opprettForespørselResponseNy.organisasjonsnumreMedStatus().forEach(organisasjonsnummerMedStatus -> {
            var orgnr = organisasjonsnummerMedStatus.organisasjonsnummerDto().orgnr();
            if (organisasjonsnummerMedStatus.status().equals(OpprettForespørselResponsNy.ForespørselResultat.FORESPØRSEL_OPPRETTET)) {
                lagHistorikkForForespørsel(ref,
                    String.format("Oppgave om å sende inntektsmelding er opprettet for %s.", hentArbeidsgivernavn(orgnr)));
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Fpinntektsmelding har allerede oppgave på saksnummer: {} og orgnummer: {} på stp: {} og første uttaksdato: {}",
                        ref.saksnummer(), tilMaskertNummer(orgnr), request.skjæringstidspunkt(), request.førsteUttaksdato());
                }
            }
        });
    }

    private void lagHistorikkForForespørsel(BehandlingReferanse ref, String tekst) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Min side - arbeidsgiver")
            .medBehandlingId(ref.behandlingId())
            .medFagsakId(ref.fagsakId())
            .addLinje(tekst)
            .build();

        historikkRepo.lagre(historikkinnslag);
    }

    private OpprettForespørselRequest.YtelseType mapYtelsetype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> OpprettForespørselRequest.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> OpprettForespørselRequest.YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT, ENGANGSTØNAD -> throw new IllegalArgumentException("Kan ikke opprette forespørsel for ytelsetype " + fagsakYtelseType);
        };
    }

    public void lagLukkForespørselTask(Behandling behandling, OrgNummer orgNummer, ForespørselStatus status) {
        var behandlingId = behandling.getId();
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(LukkForespørslerImTask.class));
        taskdata.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandlingId);
        if (orgNummer != null) {
            taskdata.setProperty(LukkForespørslerImTask.ORG_NUMMER, orgNummer.getId());
        }
        var gruppeId = String.format(GRUPPE_ID, behandling.getSaksnummer().getVerdi());
        taskdata.setGruppe(gruppeId);
        taskdata.setSekvens(String.valueOf(Instant.now().toEpochMilli()));
        taskdata.setProperty(LukkForespørslerImTask.STATUS, status.name());
        taskdata.setProperty(LukkForespørslerImTask.SAK_NUMMER, behandling.getSaksnummer().getVerdi());
        prosessTaskTjeneste.lagre(taskdata);
    }

    public void lukkForespørsel(String saksnummer, String orgnummer) {
        if (!OrganisasjonsNummerValidator.erGyldig(orgnummer)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("FpInntektsmeldingTjeneste: Lukker ikke forespørsel for saksnummer: {} fordi orgnummer: {} ikke er gyldig", saksnummer, tilMaskertNummer(orgnummer));
            }
            return;
        }
        var request = new LukkForespørselRequest(new OrganisasjonsnummerDto(orgnummer), new SaksnummerDto(saksnummer));
        klient.lukkForespørsel(request);
    }

    public void settForespørselTilUtgått(String saksnummer) {
        var request = new LukkForespørselRequest(null, new SaksnummerDto(saksnummer));
        klient.settForespørselTilUtgått(request);
    }

    public SendNyBeskjedResponse sendNyBeskjedTilArbeidsgiver(BehandlingReferanse ref, String orgnummer) {
        var request = new NyBeskjedRequest(new OrganisasjonsnummerDto(orgnummer), new SaksnummerDto(ref.saksnummer().getVerdi()));
        return klient.sendNyBeskjedPåForespørsel(request);
    }

    private String hentArbeidsgivernavn(String ag) {
        var virksomhet = arbeidsgiverTjeneste.hentVirksomhet(ag);
        return String.format("%s (%s)", virksomhet.getNavn(), virksomhet.getOrgnr());
    }
}
