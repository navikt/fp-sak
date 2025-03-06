package no.nav.foreldrepenger.domene.vedtak.migrering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.svp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

@ApplicationScoped
public class MigrerManglendeForespørslerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(MigrerManglendeForespørslerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private MaksDatoUttakTjenesteImpl maksDatoUttakTjenesteSvp;

    public MigrerManglendeForespørslerTjeneste() {
        // for CDI proxy
    }

    @Inject
    public MigrerManglendeForespørslerTjeneste(BehandlingRepository behandlingRepository, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                               StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                               UttakInputTjeneste uttakInputTjeneste,
                                               FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste,
                                               InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                               @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) MaksDatoUttakTjenesteImpl maksDatoUttakTjenesteSvp) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.maksDatoUttakTjenesteSvp = maksDatoUttakTjenesteSvp;
    }

    public void migrerManglendeForespørsler() {
        // implementasjon
    }

    public void vurderOmForespørselSkalOpprettes(Fagsak sak, boolean dryRun) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(sak.getId());
        var sisteYtelsebehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(sak.getId());

        sisteYtelsebehandling.ifPresent(behandling -> {
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsak().getYtelseType())) {
                var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput);
                var stønadRest = stønadskontoSaldoTjeneste.finnStønadRest(saldoUtregning);
                if (stønadRest <= 10) {
                    LOG.info("Saksnummer {} har ikke nok dager igjen på saldo til å opprette forespørsel. Stønadrest er: {}", sak.getSaksnummer(), stønadRest);
                    return;
                }
            } else {
                var sisteUttaksdato = maksDatoUttakTjenesteSvp.beregnMaksDatoUttak(uttakInput).orElse(Tid.TIDENES_BEGYNNELSE);
                if (sisteUttaksdato.plusDays(10).isBefore(LocalDate.now())) {
                    LOG.info("Saksnummer {} har ingen uttaksperioder. Ingen forespørsel opprettes", sak.getSaksnummer());
                    return;
                }
            }

            var arbeidsgivereMedRefusjon = inntektsmeldingTjeneste.hentInntektsmeldinger(BehandlingReferanse.fra(behandling), stp.getUtledetSkjæringstidspunkt()).stream()
                    .filter(inntektsmelding -> !inntektsmelding.kommerFraArbeidsgiverPortal() && inntektsmelding.getArbeidsgiver().getErVirksomhet())
                    .filter(inntektsmelding -> inntektsmelding.getRefusjonBeløpPerMnd() != null && inntektsmelding.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) > 0)
                    .map(inntektsmelding -> new OrganisasjonsnummerDto(inntektsmelding.getArbeidsgiver().getOrgnr()))
                    .toList();

            if (!arbeidsgivereMedRefusjon.isEmpty()) {
                LOG.info("det opprettes forespørsel for {} for følgende organisasjonsnumre {}", sak.getSaksnummer(), arbeidsgivereMedRefusjon.stream().map(OrganisasjonsnummerDto::toString).collect(Collectors.joining(", ")));
                fpInntektsmeldingTjeneste.opprettForespørsel(BehandlingReferanse.fra(behandling), stp, arbeidsgivereMedRefusjon, dryRun);
            } else {
                LOG.info("{} har ingen arbeidsgivere med refusjon", sak.getSaksnummer());
            }
        });
    }
}
