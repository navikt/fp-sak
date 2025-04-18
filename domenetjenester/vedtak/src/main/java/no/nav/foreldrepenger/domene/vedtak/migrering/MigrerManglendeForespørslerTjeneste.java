package no.nav.foreldrepenger.domene.vedtak.migrering;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.svp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
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
    private BeregningsresultatRepository tilkjentRepository;

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
                                               @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) MaksDatoUttakTjenesteImpl maksDatoUttakTjenesteSvp,
                                               BeregningsresultatRepository tilkjentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.maksDatoUttakTjenesteSvp = maksDatoUttakTjenesteSvp;
        this.tilkjentRepository = tilkjentRepository;
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
                LOG.info("MIGRER-FP: Ingen forespørsel opprettes for saksnummer {}. Finner ikke skjæringstidspunkt", sak.getSaksnummer());
                return;
            }

            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsak().getYtelseType())) {
                ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
                if (fpGrunnlag == null) {
                    LOG.info("MIGRER-FP: Ingen forespørsel opprettes for saksnummer {}. Ingen grunnlag for fp-saken", sak.getSaksnummer());
                    return;
                }
                var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput);
                var restSaldo = beregnRestSaldoForRolle(saldoUtregning, RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType()));
                var barnetsFødselsdato = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFødselsdato().orElse(Tid.TIDENES_BEGYNNELSE);
                var sisteTilkjenteUtbetalingsdato = hentSisteUtbetalingDato(behandling.getId()).orElse(Tid.TIDENES_BEGYNNELSE);
                var barnetErOver3År  = barnetsFødselsdato.plusYears(3).plusDays(1).isBefore(LocalDate.now());
                var harUtbetaltHeleSaldoen = restSaldo < 2 && sisteTilkjenteUtbetalingsdato.isBefore(LocalDate.now());

                if (harUtbetaltHeleSaldoen || barnetErOver3År){
                    LOG.info("MIGRER-FP: Ingen forespørsel opprettes for saksnummer {}. Fp-saken har ikke uttaksdager igjen, eller barnet er over 3 år. ",
                        sak.getSaksnummer());
                    return;
                }
            } else {
                var sisteUttaksdato = maksDatoUttakTjenesteSvp.beregnMaksDatoUttak(uttakInput).orElse(Tid.TIDENES_BEGYNNELSE);
                if (sisteUttaksdato.isBefore(LocalDate.now())) {
                    LOG.info("MIGRER-FP: Ingen forespørsel opprettes for saksnummer {}. Svp-saken har ikke uttaksdager igjen. ", sak.getSaksnummer());
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

    private int beregnRestSaldoForRolle(SaldoUtregning saldoUtregning, boolean erMor) {
        var stønadskontoType = erMor ? Stønadskontotype.MØDREKVOTE : Stønadskontotype.FEDREKVOTE;
        return saldoUtregning.saldo(stønadskontoType) + saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE)
            + saldoUtregning.saldo(Stønadskontotype.FORELDREPENGER);
    }

    private Optional<LocalDate> hentSisteUtbetalingDato(Long behandlingId) {
        return tilkjentRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder());
    }
}
