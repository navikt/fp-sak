package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class FpInntektsmeldingTjeneste {
    private FpinntektsmeldingKlient klient;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private HistorikkRepository historikkRepo;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    FpInntektsmeldingTjeneste() {
        // CDI
    }

    @Inject
    public FpInntektsmeldingTjeneste(FpinntektsmeldingKlient klient,
                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     HistorikkRepository historikkRepo,
                                     ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.klient = klient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkRepo = historikkRepo;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public void lagForespørselTask(String ag, BehandlingReferanse ref) {
        // Toggler av for prod og lokalt, ikke støtte lokalt
        if (!Environment.current().isDev()) {
            return;
        }
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(FpinntektsmeldingTask.class));
        taskdata.setBehandling(ref.fagsakId(), ref.behandlingId());
        taskdata.setCallIdFraEksisterende();
        taskdata.setProperty(FpinntektsmeldingTask.ARBEIDSGIVER_KEY, ag);
        prosessTaskTjeneste.lagre(taskdata);
    }

    public void overstyrInntektsmelding(Inntektsmelding inntektsmeldingSomSkalOverstyres,
                                        LocalDate opphørFom,
                                        String saksbehandlerIdent,
                                        BehandlingReferanse ref) {
        var ytelse = ref.fagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) ? OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER : OverstyrInntektsmeldingRequest.YtelseType.SVANGERSKAPSPENGER;
        var startdato = inntektsmeldingSomSkalOverstyres.getStartDatoPermisjon()
            .orElseGet(() -> skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId()).getUtledetSkjæringstidspunkt());
        var refusjon = Optional.ofNullable(inntektsmeldingSomSkalOverstyres.getRefusjonBeløpPerMnd()).map(Beløp::getVerdi).orElse(null);
        var arbeidsgiver = new OverstyrInntektsmeldingRequest.ArbeidsgiverDto(
            inntektsmeldingSomSkalOverstyres.getArbeidsgiver().getIdentifikator());
        var aktørId = new OverstyrInntektsmeldingRequest.AktørIdDto(ref.aktørId().getId());
        var endringerIRefusjon = mapRefusjonsendringer(inntektsmeldingSomSkalOverstyres.getEndringerRefusjon(), opphørFom);
        var naturalytelser = mapNaturalytelser(inntektsmeldingSomSkalOverstyres.getNaturalYtelser());
        var request = new OverstyrInntektsmeldingRequest(aktørId, arbeidsgiver, startdato, ytelse,
            inntektsmeldingSomSkalOverstyres.getInntektBeløp().getVerdi(), refusjon, endringerIRefusjon, naturalytelser, saksbehandlerIdent);
        klient.overstyrInntektsmelding(request);
    }

    private List<OverstyrInntektsmeldingRequest.BortfaltNaturalytelseRequestDto> mapNaturalytelser(List<NaturalYtelse> naturalYtelser) {
        return naturalYtelser.stream()
            .map(n -> new OverstyrInntektsmeldingRequest.BortfaltNaturalytelseRequestDto(n.getPeriode().getFomDato(), n.getPeriode().getTomDato(), n.getType(), n.getBeloepPerMnd().getVerdi()))
            .toList();
    }

    private List<OverstyrInntektsmeldingRequest.RefusjonendringRequestDto> mapRefusjonsendringer(List<Refusjon> endringerRefusjon, LocalDate opphørFom) {
        // Endringer etter opphørsdato er ikke relevant
        var endringer = endringerRefusjon.stream()
            .filter(e -> e.getFom().isBefore(opphørFom))
            .map(e -> new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(e.getFom(), e.getRefusjonsbeløp().getVerdi()))
            .collect(Collectors.toList());
        // Setter opphør
        endringer.add(new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(opphørFom, BigDecimal.ZERO));
        return endringer;
    }

    void lagForespørsel(String ag, BehandlingReferanse ref, Skjæringstidspunkt stp) {
        // Toggler av for prod og lokalt, ikke støtte lokalt
        if (!OrganisasjonsNummerValidator.erGyldig(ag)) {
            return;
        }
        var request = new OpprettForespørselRequest(new OpprettForespørselRequest.AktørIdDto(ref.aktørId().getId()),
            new OpprettForespørselRequest.OrganisasjonsnummerDto(ag), stp.getUtledetSkjæringstidspunkt(), mapYtelsetype(ref.fagsakYtelseType()),
            new OpprettForespørselRequest.SaksnummerDto(ref.saksnummer().getVerdi()));
        klient.opprettForespørsel(request);
        lagHistorikkForForespørsel(ag, ref);
    }

    private void lagHistorikkForForespørsel(String ag, BehandlingReferanse ref) {
        var virksomhet = arbeidsgiverTjeneste.hentVirksomhet(ag);
        var agNavn = String.format("%s (%s)", virksomhet.getNavn(), virksomhet.getOrgnr());
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandlingId(ref.behandlingId());
        historikkinnslag.setFagsakId(ref.fagsakId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.MIN_SIDE_ARBEIDSGIVER);

        var beg = String.format("Bedt %s om å sende inntektsmelding", agNavn);
        new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.MIN_SIDE_ARBEIDSGIVER)
            .medBegrunnelse(beg)
            .build(historikkinnslag);
        historikkRepo.lagre(historikkinnslag);
    }

    private OpprettForespørselRequest.YtelseType mapYtelsetype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> OpprettForespørselRequest.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> OpprettForespørselRequest.YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT,ENGANGSTØNAD -> throw new IllegalArgumentException("Kan ikke opprette forespørsel for ytelsetype " + fagsakYtelseType);
        };
    }

}
