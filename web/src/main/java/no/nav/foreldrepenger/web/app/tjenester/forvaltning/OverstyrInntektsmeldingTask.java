package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/*
 * Overstyring av inntektsmeldingtask. Gjøres i egen task for å sikre systemkontekst
 */
@Dependent
@ProsessTask(value = "fpinntektsmelding.overstyr.inntektsmelding", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class OverstyrInntektsmeldingTask implements ProsessTaskHandler {

    public static final String JOURNALPOST_ID = "journalpostId";
    public static final String BEHANDLING_ID = "behandlingId";
    public static final String OPPHØR_FOM = "opphoersdato";
    public static final String SAKSBEHANDLER_IDENT = "saksbehandlerIdent";

    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;
    private final BehandlingRepository behandlingRepository;

    @Inject
    public OverstyrInntektsmeldingTask(InntektArbeidYtelseTjeneste iayTjeneste,
                                       FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste,
                                       BehandlingRepository behandlingRepository) {
        this.iayTjeneste = iayTjeneste;
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        var jpId = Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNALPOST_ID)).map(JournalpostId::new).orElseThrow();
        var behandlingId = Optional.ofNullable(prosessTaskData.getPropertyValue(BEHANDLING_ID)).map(Long::valueOf).orElseThrow();
        var opphørFom = LocalDate.parse(prosessTaskData.getPropertyValue(OPPHØR_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        var saksbehandlerIdent = Optional.ofNullable(prosessTaskData.getPropertyValue(SAKSBEHANDLER_IDENT)).orElseThrow();
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));

        var inntektsmeldingSomSkalOverstyres = iayTjeneste.finnGrunnlag(behandlingId)
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(List.of())
            .stream()
            .filter(im -> im.getJournalpostId().equals(jpId))
            .findFirst()
            .orElseThrow();

        fpInntektsmeldingTjeneste.overstyrInntektsmelding(inntektsmeldingSomSkalOverstyres, opphørFom, saksbehandlerIdent, ref);
    }
}
