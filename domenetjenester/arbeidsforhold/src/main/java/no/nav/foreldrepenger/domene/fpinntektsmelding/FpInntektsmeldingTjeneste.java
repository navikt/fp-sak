package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class FpInntektsmeldingTjeneste {
    private static final String GRUPPE_ID = "FPIM_TASK_%s";
    private static final int PÅMINNELSE_ETTER_DAGER = 14; // jf. MinSideArbeidsgiverTjeneste.PÅMINNELSE_ETTER_DAGER i fp-inntektsmelding
    private static final DateTimeFormatter DATO_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private FpInntektsmeldingKlient klient;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private HistorikkinnslagRepository historikkRepo;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private boolean erToggleForNyInnsendingPå = false;

    private static final Logger LOG = LoggerFactory.getLogger(FpInntektsmeldingTjeneste.class);

    public FpInntektsmeldingTjeneste() {
        // CDI
    }

    @Inject
    public FpInntektsmeldingTjeneste(FpInntektsmeldingKlient klient,
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
        // Hvis toggle er på vil det gi hyppigere bestillinger og oppdateringer av første uttaksdato for arbeidsgiver
        this.erToggleForNyInnsendingPå = !Environment.current().isProd();
    }

    public void lagTaskForespørAlleInntektsmeldinger(BehandlingReferanse ref) {
        lagTask(ref, null);
    }

    public void lagTaskForespørBestemtInntektsmelding(BehandlingReferanse ref, String orgnummer) {
        lagTask(ref, orgnummer);
    }

    public boolean erToggleForNyInnsendingPå() {
        return erToggleForNyInnsendingPå;
    }

    //Denne brukes av opprettForespørsel i ForvaltningBehandlingRestTjeneste dersom det av en eller annen grunn ikke er opprettet
    //forespørsel på en behandling. I disse tilfellene kan im allerede være mottatt (før altinn2 ble stengt).
    // Det er ikke et vanlig case og koden bør ikke brukes i andre tilfeller
    public void lagTaskForespørOgLukkBestemtInntektsmelding(Behandling behandling, String orgnummer) {
        var forespørselTask = ProsessTaskData.forTaskType(TaskType.forProsessTask(FpInntektsmeldingTask.class));
        forespørselTask.setProperty(FpInntektsmeldingTask.ORGNUMMER, orgnummer);

        var lukkTask = ProsessTaskData.forTaskType(TaskType.forProsessTask(LukkForespørslerImTask.class));
        lukkTask.setProperty(LukkForespørslerImTask.ORG_NUMMER, orgnummer);
        lukkTask.setProperty(LukkForespørslerImTask.STATUS, ForespørselStatus.UTFØRT.name());
        lukkTask.setProperty(LukkForespørslerImTask.SAK_NUMMER, behandling.getSaksnummer().getVerdi());

        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.addNesteSekvensiell(forespørselTask);
        taskGruppe.addNesteSekvensiell(lukkTask);
        taskGruppe.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        prosessTaskTjeneste.lagre(taskGruppe);
    }

    private void lagTask(BehandlingReferanse ref, String orgnummer) {
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(FpInntektsmeldingTask.class));
        taskdata.setBehandling(ref.saksnummer().getVerdi(), ref.fagsakId(), ref.behandlingId());
        if (orgnummer != null) {
            taskdata.setProperty(FpInntektsmeldingTask.ORGNUMMER, orgnummer);
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

    public void lagForespørselForBestemtArbeidsgiver(BehandlingReferanse ref, Skjæringstidspunkt stp, Arbeidsgiver arbeidsgiver) {
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var førsteUttaksdato = stp.getFørsteUttaksdato();
        var agDto = new OrganisasjonsnummerDto(arbeidsgiver.getOrgnr());

        if (erToggleForNyInnsendingPå) {
            // Togglet på i dev: Nytt endepunkt som aksepterer og forventer kun ett orgnr
            var request = new OpprettEnForespørselRequest(new AktørIdDto(ref.aktørId().getId()), skjæringstidspunkt,
                mapYtelsetype(ref.fagsakYtelseType()), new SaksnummerDto(ref.saksnummer().getVerdi()), førsteUttaksdato, agDto);
            sendEnkeltRequest(ref, request);
        } else {
            var request = new OpprettForespørselRequest(new AktørIdDto(ref.aktørId().getId()), null, skjæringstidspunkt,
                mapYtelsetype(ref.fagsakYtelseType()), new SaksnummerDto(ref.saksnummer().getVerdi()), førsteUttaksdato,
                List.of(agDto));

            sendRequest(ref, request);
        }
    }

    public void lagForespørselForAlleArbeidsgivere(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var førsteUttaksdato = stp.getFørsteUttaksdato();

        if (erToggleForNyInnsendingPå) {
            // Togglet på i dev: Ny innsending som alltid sender komplett liste over forespørsler, også de som er mottatte
            var arbeidsgivereViManglerInntektsmeldingFra = inntektsmeldingRegisterTjeneste.utledAllePåKrevdeInntektsmeldinger(ref, stp)
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
            var request = new OpprettKomplettForespørslerRequest(new AktørIdDto(ref.aktørId().getId()), skjæringstidspunkt,
                mapYtelsetype(ref.fagsakYtelseType()), new SaksnummerDto(ref.saksnummer().getVerdi()), førsteUttaksdato,
                arbeidsgivereViManglerInntektsmeldingFra);
            sendKomplettRequest(ref, request);
        } else {
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
            var request = new OpprettForespørselRequest(new AktørIdDto(ref.aktørId().getId()), null, skjæringstidspunkt,
                mapYtelsetype(ref.fagsakYtelseType()), new SaksnummerDto(ref.saksnummer().getVerdi()), førsteUttaksdato,
                arbeidsgivereViManglerInntektsmeldingFra);
            sendRequest(ref, request);
        }
    }

    private void sendRequest(BehandlingReferanse ref,
                             OpprettForespørselRequest request) {
        LOG.info(
            "Sender kall til fpinntektsmelding om å opprette forespørsel for saksnummer {} med skjæringstidspunkt {} for følgende organisasjonsnumre: {}",
            ref.saksnummer(), request.skjæringstidspunkt(), request.organisasjonsnumre());

        var opprettForespørselResponseNy = klient.opprettForespørsel(request);

        var arbeidsgivereMedNyForespørsel = new ArrayList<String>();
        opprettForespørselResponseNy.organisasjonsnumreMedStatus().forEach(organisasjonsnummerMedStatus -> {
            var orgnr = organisasjonsnummerMedStatus.organisasjonsnummerDto().orgnr();
            if (organisasjonsnummerMedStatus.status().equals(OpprettForespørselRespons.ForespørselResultat.FORESPØRSEL_OPPRETTET)) {
                arbeidsgivereMedNyForespørsel.add(hentArbeidsgivernavn(orgnr));
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Fpinntektsmelding har allerede oppgave på saksnummer: {} og orgnummer: {} på stp: {} og første uttaksdato: {}",
                        ref.saksnummer(), tilMaskertNummer(orgnr), request.skjæringstidspunkt(), request.førsteUttaksdato());
                }
            }
        });
        if (!arbeidsgivereMedNyForespørsel.isEmpty()) {
            lagHistorikkForForespørsel(ref, arbeidsgivereMedNyForespørsel, List.of(), request.førsteUttaksdato());
        }
    }

    private void sendEnkeltRequest(BehandlingReferanse ref,
                             OpprettEnForespørselRequest request) {
        LOG.info(
            "Sender kall til fpinntektsmelding om å opprette forespørsel for saksnummer {} med skjæringstidspunkt {} for følgende organisasjonsnumre: {}",
            ref.saksnummer(), request.skjæringstidspunkt(), request.orgnummer());

        var statusResponse = klient.opprettSpesifikkForespørsel(request);

        opprettHistorikkInnslagOmNødvendig(ref, request.skjæringstidspunkt(), request.førsteUttaksdato(), List.of(statusResponse));
    }

    private void sendKomplettRequest(BehandlingReferanse ref,
                             OpprettKomplettForespørslerRequest request) {
        LOG.info(
            "Sender kall til fpinntektsmelding om å opprette forespørsel for saksnummer {} med skjæringstidspunkt {} for følgende organisasjonsnumre: {}",
            ref.saksnummer(), request.skjæringstidspunkt(), request.organisasjonsnummer());

        var opprettForespørselResponse = klient.opprettForespørselKomplett(request);

        opprettHistorikkInnslagOmNødvendig(ref, request.skjæringstidspunkt(), request.førsteUttaksdato(), opprettForespørselResponse.organisasjonsnumreMedStatus());
    }

    private void opprettHistorikkInnslagOmNødvendig(BehandlingReferanse ref, LocalDate skjæringstidspunkt, LocalDate førsteUttaksdato,
                                                    List<OpprettForespørselRespons.OrganisasjonsnummerMedStatus> organisasjonsnummerMedStatuses) {
        var arbeidsgivereMedNyForespørsel = new ArrayList<String>();
        var arbeidsgivereMedEndretForespørsel = new ArrayList<String>();
        organisasjonsnummerMedStatuses.forEach(organisasjonsnummerMedStatus -> {
            var orgnr = organisasjonsnummerMedStatus.organisasjonsnummerDto().orgnr();
            if (organisasjonsnummerMedStatus.status().equals(OpprettForespørselRespons.ForespørselResultat.FORESPØRSEL_OPPRETTET)) {
                arbeidsgivereMedNyForespørsel.add(hentArbeidsgivernavn(orgnr));
            } else if (organisasjonsnummerMedStatus.status().equals(OpprettForespørselRespons.ForespørselResultat.FORESPØRSEL_ENDRET)) {
                arbeidsgivereMedEndretForespørsel.add(hentArbeidsgivernavn(orgnr));
            } else {
                LOG.info("Fpinntektsmelding opprettet ikke forespørsel på saksnummer: {} og orgnummer: {} på stp: {} og første uttaksdato: {}. Grunnen var: {}",
                    ref.saksnummer(), tilMaskertNummer(orgnr), skjæringstidspunkt, førsteUttaksdato, organisasjonsnummerMedStatus.status());
            }
        });
        if (!arbeidsgivereMedNyForespørsel.isEmpty() || !arbeidsgivereMedEndretForespørsel.isEmpty()) {
            lagHistorikkForForespørsel(ref, arbeidsgivereMedNyForespørsel, arbeidsgivereMedEndretForespørsel, førsteUttaksdato);
        }
    }


    private void lagHistorikkForForespørsel(BehandlingReferanse ref, List<String> arbeidsgivereMedNyForespørsel,
                                            List<String> arbeidsgivereMedEndretForespørsel,LocalDate førsteUttaksdato) {
        var ytelse = ref.fagsakYtelseType().getNavn().toLowerCase(Locale.ROOT);
        var builder = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Forespørsel om inntektsmelding")
            .medBehandlingId(ref.behandlingId())
            .medFagsakId(ref.fagsakId());
        var historikkinnslagBuilder = builder
            .addLinje("Varslet på Min side - arbeidsgiver og Altinn innboks.")
            .addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        if (!arbeidsgivereMedNyForespørsel.isEmpty()) {
            var oppsummeringTekst = String.format(
                "Arbeidsgiver er informert om at startdato for %s er %s. Påminnelse sendes automatisk til arbeidsgiver dersom inntektsmelding ikke er mottatt innen %d dager.",
                ytelse, førsteUttaksdato.format(DATO_FORMAT), PÅMINNELSE_ETTER_DAGER);
            arbeidsgivereMedNyForespørsel.forEach(builder::addLinje);
            historikkinnslagBuilder.addLinje(oppsummeringTekst);
        }
        if (!arbeidsgivereMedEndretForespørsel.isEmpty()) {
            var oppsummeringTekst = String.format(
                "Arbeidsgiver er informert om at ny startdato for %s er %s.",
                ytelse, førsteUttaksdato.format(DATO_FORMAT), PÅMINNELSE_ETTER_DAGER);
            arbeidsgivereMedEndretForespørsel.forEach(builder::addLinje);
            historikkinnslagBuilder.addLinje(oppsummeringTekst);
        }
        historikkRepo.lagre(historikkinnslagBuilder.build());
    }

    private FpInntektsmeldingYtelse mapYtelsetype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> FpInntektsmeldingYtelse.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> FpInntektsmeldingYtelse.SVANGERSKAPSPENGER;
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
