package no.nav.foreldrepenger.domene.vedtak.migrering;

import java.time.LocalDate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public MigrerManglendeForespørslerTjeneste(BehandlingRepository behandlingRepository,
                                               SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
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
        var sisteYtelsebehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(sak.getId());

        sisteYtelsebehandling.ifPresent(behandling -> {
            var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            if (stp == null) {
                LOG.info("MIGRER-FP: Saksnummer {} har ingen skjæringstidspunkt. Ingen forespørsel opprettes", sak.getSaksnummer());
                return;
            }
            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsak().getYtelseType())) {
                var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput);
                var stønadRest = stønadskontoSaldoTjeneste.finnStønadRest(saldoUtregning);
                if (stønadRest < 1) {
                    LOG.info("MIGRER-FP: Saksnummer {} har ikke nok dager igjen på saldo til å opprette forespørsel. Stønadrest er: {}",
                        sak.getSaksnummer(), stønadRest);
                    return;
                }
            } else {
                var sisteUttaksdato = maksDatoUttakTjenesteSvp.beregnMaksDatoUttak(uttakInput).orElse(Tid.TIDENES_BEGYNNELSE);
                if (sisteUttaksdato.plusDays(10).isBefore(LocalDate.now())) {
                    LOG.info("MIGRER-FP: Saksnummer {} har ingen uttaksperioder. Ingen forespørsel opprettes", sak.getSaksnummer());
                    return;
                }
            }

            var arbeidsgivereSomHarSendtIm = inntektsmeldingTjeneste.hentInntektsmeldinger(BehandlingReferanse.fra(behandling),
                    stp.getUtledetSkjæringstidspunkt())
                .stream()
                .filter(inntektsmelding -> !inntektsmelding.kommerFraArbeidsgiverPortal() && inntektsmelding.getArbeidsgiver().getErVirksomhet())
                .map(inntektsmelding -> new OrganisasjonsnummerDto(inntektsmelding.getArbeidsgiver().getOrgnr()))
                .toList();

            if (!arbeidsgivereSomHarSendtIm.isEmpty()) {
                LOG.info("MIGRER-FP: Det opprettes migrert forespørsel for {} for følgende organisasjonsnumre {}", sak.getSaksnummer(),
                    arbeidsgivereSomHarSendtIm.stream().map(OrganisasjonsnummerDto::toString).collect(Collectors.joining(", ")));
                fpInntektsmeldingTjeneste.opprettMigrertForespørsel(BehandlingReferanse.fra(behandling), stp, arbeidsgivereSomHarSendtIm, dryRun);
            } else {
                LOG.info("MIGRER-FP: {} har ingen arbeidsgivere med refusjon", sak.getSaksnummer());
            }
        });
    }
}
