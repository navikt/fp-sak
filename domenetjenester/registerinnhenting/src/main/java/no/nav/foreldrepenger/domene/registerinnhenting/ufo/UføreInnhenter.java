package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class UføreInnhenter {

    private UføretrygdRepository uføretrygdRepository;
    private YtelsesFordelingRepository yfRepository;
    private PersonopplysningRepository poRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private PesysUføreKlient pesysUføreKlient;

    UføreInnhenter() {
        // for CDI proxy
    }

    @Inject
    public UføreInnhenter(UføretrygdRepository uføretrygdRepository,
                          YtelsesFordelingRepository yfRepository,
                          PersonopplysningRepository poRepository,
                          ForeldrepengerUttakTjeneste uttakTjeneste,
                          PersoninfoAdapter personinfoAdapter,
                          PesysUføreKlient pesysUføreKlient) {
        this.uføretrygdRepository = uføretrygdRepository;
        this.yfRepository = yfRepository;
        this.poRepository = poRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.pesysUføreKlient = pesysUføreKlient;
    }

    public void innhentUføretrygd(Behandling behandling) {
        // 14-14 Bare Far/Medmor har rett
        if (!FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) ||
            RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
            return;
        }
        var annenpartAktørId = poRepository.hentOppgittAnnenPartHvisEksisterer(behandling.getId())
            .map(OppgittAnnenPartEntitet::getAktørId).orElse(null);
        var harGrunnlagForAnnenPart = uføretrygdRepository.hentGrunnlag(behandling.getId())
            .filter(g -> annenpartAktørId != null && annenpartAktørId.equals(g.getAktørIdAnnenPart())).isPresent();
        // Mangler annenpart eller har allerede innhentet Venter ikke oppdatering av uførestatus
        if (annenpartAktørId == null || harGrunnlagForAnnenPart) {
            return;
        }

        var yfAggregat = yfRepository.hentAggregatHvisEksisterer(behandling.getId());
        var finnesPerioderMedMorAktivitetUfør = yfAggregat.map(YtelseFordelingAggregat::getGjeldendeFordeling)
            .map(OppgittFordelingEntitet::getPerioder).orElse(List.of()).stream()
            .filter(p -> UttakPeriodeType.FORELDREPENGER.equals(p.getPeriodeType()))
            .anyMatch(p -> MorsAktivitet.UFØRE.equals(p.getMorsAktivitet()));
        var oppgittRettighetMorUfør = yfAggregat.map(YtelseFordelingAggregat::getOppgittRettighet)
            .filter(r -> Objects.equals(r.getMorMottarUføretrygd(), Boolean.TRUE))
            .isPresent();
        // Midlertidig: Sjekker også om det finnes innvilgete perioder for å opprette grunnlag for eksisterende
        var harInnvilgetUttakMedUføre = uttakResultatMedInnvilgetUføre(behandling);
        if (finnesPerioderMedMorAktivitetUfør || oppgittRettighetMorUfør || harInnvilgetUttakMedUføre) {
            var førsteUttakDato = førsteUttaksdag(behandling, yfAggregat);
            innhentOgLagre(behandling, annenpartAktørId, førsteUttakDato);
        }
    }

    private void innhentOgLagre(Behandling behandling, AktørId annenpartAktørId, LocalDate startDato) {
        var uføreperiode = personinfoAdapter.hentFnr(annenpartAktørId)
            .flatMap(fnr -> pesysUføreKlient.hentUføreHistorikk(fnr.getIdent(), startDato));
        uføretrygdRepository.lagreUføreGrunnlagRegisterVersjon(behandling.getId(), annenpartAktørId, uføreperiode.isPresent(),
            uføreperiode.map(Uføreperiode::uforetidspunkt).orElse(null), uføreperiode.map(Uføreperiode::virkningsdato).orElse(null));
    }

    private LocalDate førsteUttaksdag(Behandling behandling, Optional<YtelseFordelingAggregat> ytelseFordeling) {

        var førsteUttaksdagSøknad = ytelseFordeling.map(YtelseFordelingAggregat::getGjeldendeFordeling)
            .map(OppgittFordelingEntitet::getPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder())
            .orElse(Tid.TIDENES_ENDE);

        var førsteUttaksdagForrigeVedtak = finnFørsteDatoIUttakResultat(behandling).orElse(Tid.TIDENES_ENDE);
        var førsteUttaksdag = førsteUttaksdagSøknad.isBefore(førsteUttaksdagForrigeVedtak) ? førsteUttaksdagSøknad : førsteUttaksdagForrigeVedtak;
        return førsteUttaksdag.equals(Tid.TIDENES_ENDE) ? LocalDate.now() : førsteUttaksdag;
    }

    private Optional<LocalDate> finnFørsteDatoIUttakResultat(Behandling behandling) {
        if (!BehandlingType.REVURDERING.equals(behandling.getType())) return Optional.empty();
        return uttakTjeneste.hentHvisEksisterer(originalBehandling(behandling))
            .map(ForeldrepengerUttak::getGjeldendePerioder).orElse(List.of()).stream()
            .map(ForeldrepengerUttakPeriode::getFom)
            .min(Comparator.naturalOrder());
    }

    private boolean uttakResultatMedInnvilgetUføre(Behandling behandling) {
        return BehandlingType.REVURDERING.equals(behandling.getType()) &&
            uttakTjeneste.hentHvisEksisterer(originalBehandling(behandling))
            .map(ForeldrepengerUttak::getGjeldendePerioder).orElse(List.of()).stream()
            .filter(ForeldrepengerUttakPeriode::isInnvilget)
            .anyMatch(p -> MorsAktivitet.UFØRE.equals(p.getMorsAktivitet()));
    }

    private Long originalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalArgumentException("Revurdering må ha original behandling"));
    }

}
