package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.PeriodeKonverter;

@ApplicationScoped
public class YtelseFordelingDtoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste;

    YtelseFordelingDtoTjeneste() {
        //CDI
    }

    @Inject
    public YtelseFordelingDtoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                          FagsakRelasjonRepository fagsakRelasjonRepository,
                                          FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.førsteUttaksdatoTjeneste = førsteUttaksdatoTjeneste;
    }

    public Optional<YtelseFordelingDto> mapFra(Behandling behandling) {
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        YtelseFordelingDto.Builder dtoBuilder = new YtelseFordelingDto.Builder();
        if (ytelseFordelingAggregat.isPresent()) {
            Optional<PerioderUtenOmsorgEntitet> perioderUtenOmsorg = ytelseFordelingAggregat.get().getPerioderUtenOmsorg();
            Optional<PerioderAleneOmsorgEntitet> perioderAleneOmsorg = ytelseFordelingAggregat.get().getPerioderAleneOmsorg();
            Optional<AvklarteUttakDatoerEntitet> avklarteUttakDatoerOpt = ytelseFordelingAggregat.get().getAvklarteDatoer();
            Optional<PerioderAnnenforelderHarRettEntitet> perioderAnnenforelderHarRett = ytelseFordelingAggregat.get().getPerioderAnnenforelderHarRett();

            if (perioderAleneOmsorg.isPresent()) {
                List<PeriodeAleneOmsorgEntitet> periodeAleneOmsorgs = perioderAleneOmsorg.get().getPerioder();
                dtoBuilder.medAleneOmsorgPerioder(PeriodeKonverter.mapAleneOmsorgsperioder(periodeAleneOmsorgs));
            }
            if (perioderUtenOmsorg.isPresent()) {
                List<PeriodeUtenOmsorgEntitet> periodeUtenOmsorgs = perioderUtenOmsorg.get().getPerioder();
                dtoBuilder.medIkkeOmsorgPerioder(PeriodeKonverter.mapUtenOmsorgperioder(periodeUtenOmsorgs));
            }
            if (avklarteUttakDatoerOpt.isPresent()) {
                dtoBuilder.medEndringsdato(avklarteUttakDatoerOpt.get().getGjeldendeEndringsdato());
            }
            leggTilFørsteUttaksdato(behandling, dtoBuilder);
            lagAnnenforelderHarRettDto(behandling, perioderAnnenforelderHarRett, dtoBuilder);
        }
        fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak()).ifPresent(fagsakRelasjon1 -> dtoBuilder.medGjeldendeDekningsgrad(fagsakRelasjon1.getGjeldendeDekningsgrad().getVerdi()));
        return Optional.of(dtoBuilder.build());
    }

    private void leggTilFørsteUttaksdato(Behandling behandling, YtelseFordelingDto.Builder dtoBuilder) {
        Optional<LocalDate> førsteUttaksdato = førsteUttaksdatoTjeneste.finnFørsteUttaksdato(behandling);
        if (førsteUttaksdato.isPresent()) {
            dtoBuilder.medFørsteUttaksdato(førsteUttaksdato.get());
        }
    }

    private void lagAnnenforelderHarRettDto(Behandling behandling, Optional<PerioderAnnenforelderHarRettEntitet> perioderAnnenforelderHarRett, YtelseFordelingDto.Builder dtoBuilder) {
        AnnenforelderHarRettDto annenforelderHarRettDto = new AnnenforelderHarRettDto();
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT))
            .findFirst();
        aksjonspunkt.ifPresent(akspkt -> annenforelderHarRettDto.setBegrunnelse(akspkt.getBegrunnelse()));
        if (perioderAnnenforelderHarRett.isPresent()) {
            List<PeriodeAnnenforelderHarRettEntitet> periodeAnnenforelderHarRett = perioderAnnenforelderHarRett.get().getPerioder();
            annenforelderHarRettDto.setAnnenforelderHarRett(!periodeAnnenforelderHarRett.isEmpty());
            annenforelderHarRettDto.setAnnenforelderHarRettPerioder(PeriodeKonverter.mapAnnenforelderHarRettPerioder(periodeAnnenforelderHarRett));
        }
        dtoBuilder.medAnnenforelderHarRett(annenforelderHarRettDto);
    }

}
