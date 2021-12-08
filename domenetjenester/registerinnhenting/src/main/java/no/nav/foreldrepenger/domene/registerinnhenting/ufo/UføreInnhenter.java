package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;

@ApplicationScoped
public class UføreInnhenter {

    private YtelsesFordelingRepository yfRepository;
    private PersonopplysningRepository poRepository;
    private PersoninfoAdapter personinfoAdapter;
    private PesysUføreKlient pesysUføreKlient;

    UføreInnhenter() {
        // for CDI proxy
    }

    @Inject
    public UføreInnhenter(YtelsesFordelingRepository yfRepository,
                          PersonopplysningRepository poRepository,
                          PersoninfoAdapter personinfoAdapter,
                          PesysUføreKlient pesysUføreKlient) {
        this.yfRepository = yfRepository;
        this.poRepository = poRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.pesysUføreKlient = pesysUføreKlient;
    }

    public void innhentUføretrygd(Behandling behandling) {

        if (!FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) ||
            RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
            return;
        }
        var finnesPerioderMedMorAktivitetUfør = yfRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder)
            .map(OppgittFordelingEntitet::getOppgittePerioder).orElse(List.of()).stream()
            .filter(p -> UttakPeriodeType.FORELDREPENGER.equals(p.getPeriodeType()))
            .anyMatch(p -> MorsAktivitet.UFØRE.equals(p.getMorsAktivitet()));
        if (finnesPerioderMedMorAktivitetUfør) {
            poRepository.hentOppgittAnnenPartHvisEksisterer(behandling.getId())
                .map(OppgittAnnenPartEntitet::getAktørId)
                .flatMap(ap -> personinfoAdapter.hentFnr(ap))
                .ifPresent(fnr -> pesysUføreKlient.hentUføreHistorikk(fnr.getIdent(), behandling.getFagsak().getSaksnummer().getVerdi()));
        }
    }

}
