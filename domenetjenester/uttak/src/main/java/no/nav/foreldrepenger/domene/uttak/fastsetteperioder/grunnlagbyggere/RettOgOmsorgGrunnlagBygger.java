package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RettOgOmsorg;

@ApplicationScoped
public class RettOgOmsorgGrunnlagBygger {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    RettOgOmsorgGrunnlagBygger() {
        // CDI
    }

    @Inject
    public RettOgOmsorgGrunnlagBygger(UttakRepositoryProvider uttakRepositoryProvider, ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelsesFordelingRepository = uttakRepositoryProvider.getYtelsesFordelingRepository();
        this.uttakTjeneste = uttakTjeneste;
    }

    public RettOgOmsorg.Builder byggGrunnlag(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        var annenpartsUttaksplan = hentAnnenpartsUttak(uttakInput);
        var samtykke = samtykke(ytelseFordelingAggregat);
        var annenpartHarForeldrepengerUtbetaling = annenpartsUttaksplan.filter(ForeldrepengerUttak::harUtbetaling).isPresent();
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var rettighetstype = map(ytelseFordelingAggregat.getGjeldendeRettighetstype(annenpartHarForeldrepengerUtbetaling, ref.relasjonRolle(),
            ytelsespesifiktGrunnlag.getUføretrygdGrunnlag().orElse(null)));
        return new RettOgOmsorg.Builder()
            .rettighetstype(rettighetstype)
                .morOppgittUføretrygd(morOppgittUføretrygd(uttakInput))
                .samtykke(samtykke)
                .harOmsorg(ytelseFordelingAggregat.harOmsorg())
            ;
    }

    private no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Rettighetstype map(Rettighetstype rettighetstype) {
        return switch (rettighetstype) {
            case ALENEOMSORG -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Rettighetstype.ALENEOMSORG;
            case BEGGE_RETT, BEGGE_RETT_EØS -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Rettighetstype.BEGGE_RETT;
            case BARE_MOR_RETT -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Rettighetstype.BARE_MOR_RETT;
            case BARE_FAR_RETT -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Rettighetstype.BARE_FAR_RETT;
            case BARE_FAR_RETT_MOR_UFØR -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Rettighetstype.BARE_FAR_RETT_MOR_UFØR;
        };
    }

    private Optional<ForeldrepengerUttak> hentAnnenpartsUttak(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var annenpart = fpGrunnlag.getAnnenpart();
        if (annenpart.isEmpty()) {
            return Optional.empty();
        }
        return uttakTjeneste.hentHvisEksisterer(annenpart.get().gjeldendeVedtakBehandlingId());
    }

    private boolean morOppgittUføretrygd(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        return fpGrunnlag.getUføretrygdGrunnlag().isPresent();
    }

    private boolean samtykke(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getOppgittFordeling().getErAnnenForelderInformert();
    }
}
