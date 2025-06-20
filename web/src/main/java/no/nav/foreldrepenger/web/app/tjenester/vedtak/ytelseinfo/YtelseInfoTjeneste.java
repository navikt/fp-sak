package no.nav.foreldrepenger.web.app.tjenester.vedtak.ytelseinfo;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class YtelseInfoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(YtelseInfoTjeneste.class);

    private static final Set<VedtakResultatType> AKTUELLE_VEDTAK_RESULTAT = Set.of(VedtakResultatType.INNVILGET, VedtakResultatType.OPPHØR);

    private BehandlingVedtakRepository vedtakRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    public YtelseInfoTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public YtelseInfoTjeneste(BehandlingVedtakRepository vedtakRepository,
                              BeregningsresultatRepository tilkjentYtelseRepository,
                              FamilieHendelseRepository familieHendelseRepository) {
        this.vedtakRepository = vedtakRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    public Optional<YtelseInfoEksternResponse> genererYtelseInfo(Behandling behandling) {
        var vedtak = vedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        if (vedtak.filter(this::erInnvilget).isEmpty()) {
            return Optional.empty();
        }

        var utbetalinger = map(behandling, vedtak.orElseThrow());
        if (utbetalinger.isEmpty()) {
            return Optional.empty();
        }
        var ytelseInfo = new YtelseInfoEksternResponse(mapYtelse(behandling.getFagsakYtelseType()),
            behandling.getSaksnummer().getVerdi(), utbetalinger);


        return Optional.of(ytelseInfo);
    }

    private boolean erInnvilget(BehandlingVedtak vedtak) {
        return AKTUELLE_VEDTAK_RESULTAT.contains(vedtak.getVedtakResultatType());
    }

    private List<YtelseInfoEksternResponse.UtbetalingEksternDto> map(Behandling behandling, BehandlingVedtak vedtak) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return mapEngangsstønad(behandling, vedtak);
        } else {
            var berResultat = tilkjentYtelseRepository.hentUtbetBeregningsresultat(behandling.getId());
            return berResultat.map(this::mapBeregningsresultat).orElse(List.of());
        }
    }

    private List<YtelseInfoEksternResponse.UtbetalingEksternDto> mapBeregningsresultat(BeregningsresultatEntitet tilkjent) {
        return tilkjent.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(this::mapBeregningsresultatPeriode)
            .toList();
    }

    private YtelseInfoEksternResponse.UtbetalingEksternDto mapBeregningsresultatPeriode(BeregningsresultatPeriode periode) {
        return new YtelseInfoEksternResponse.UtbetalingEksternDto(periode.getBeregningsresultatPeriodeFom(),
            periode.getBeregningsresultatPeriodeTom(), periode.getKalkulertUtbetalingsgrad().setScale(2, RoundingMode.HALF_UP));
    }


    private YtelseInfoEksternResponse.Ytelse mapYtelse(FagsakYtelseType type) {
        return switch (type) {
            case ENGANGSTØNAD -> YtelseInfoEksternResponse.Ytelse.ENGANGSSTØNAD;
            case FORELDREPENGER -> YtelseInfoEksternResponse.Ytelse.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseInfoEksternResponse.Ytelse.SVANGERSKAPSPENGER;
            default -> throw new IllegalStateException("Ukjent ytelsestype " + type);
        };
    }

    private List<YtelseInfoEksternResponse.UtbetalingEksternDto> mapEngangsstønad(Behandling behandling, BehandlingVedtak vedtak) {
        try {
            var stp = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(a -> a.getGjeldendeVersjon().getSkjæringstidspunkt())
                .orElse(vedtak.getVedtaksdato());
            var periode = new YtelseInfoEksternResponse.UtbetalingEksternDto(stp, stp, BigDecimal.valueOf(100));
            return List.of(periode);
        } catch (Exception e) {
            LOG.warn("Finner ikke familiehendelsedato for sak {} behandling {}", behandling.getSaksnummer().getVerdi(), behandling.getId());
            return List.of();
        }
    }

}
