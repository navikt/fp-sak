package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;

class YtelsesFordelingRepositoryForTest extends YtelsesFordelingRepository {

    private final Map<Long, YtelseFordelingAggregat> ytelseFordelingAggregatMap = new ConcurrentHashMap<>();

    @Override
    public YtelseFordelingAggregat hentAggregat(Long behandlingId) {
        return hentAggregatHvisEksisterer(behandlingId).orElseThrow();
    }

    @Override
    public YtelseFordelingAggregat hentYtelsesFordelingPåId(Long aggregatId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<YtelseFordelingAggregat> hentAggregatHvisEksisterer(Long behandlingId) {
        var yf = ytelseFordelingAggregatMap.get(behandlingId);
        if (yf == null) {
            return Optional.empty();
        }
        return Optional.of(yf);
    }

    @Override
    public void lagre(Long behandlingId, OppgittRettighetEntitet oppgittRettighet) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medOppgittRettighet(oppgittRettighet)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagre(Long behandlingId, OppgittFordelingEntitet oppgittPerioder) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medOppgittFordeling(oppgittPerioder)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagreJustertFordeling(Long behandlingId,
                                      OppgittFordelingEntitet justertFordeling,
                                      AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medJustertFordeling(justertFordeling)
            .medAvklarteDatoer(avklarteUttakDatoer)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagreOverstyrtFordeling(Long behandlingId,
                                        OppgittFordelingEntitet oppgittPerioder,
                                        PerioderUttakDokumentasjonEntitet perioderUttakDokumentasjon) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medOverstyrtFordeling(oppgittPerioder)
            .medPerioderUttakDokumentasjon(perioderUttakDokumentasjon)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagreOverstyrtFordeling(Long behandlingId, OppgittFordelingEntitet oppgittPerioder) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medOverstyrtFordeling(oppgittPerioder)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagre(Long behandlingId, OppgittDekningsgradEntitet oppgittDekningsgrad) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medOppgittDekningsgrad(oppgittDekningsgrad)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagre(Long behandlingId, PerioderUtenOmsorgEntitet perioderUtenOmsorg) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medPerioderUtenOmsorg(perioderUtenOmsorg)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagre(Long behandlingId, PerioderAleneOmsorgEntitet perioderAleneOmsorg) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medPerioderAleneOmsorg(perioderAleneOmsorg)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagre(Long behandlingId, PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRett) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medPerioderAnnenforelderHarRett(perioderAnnenforelderHarRett)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void lagre(Long behandlingId, AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        var ny = YtelseFordelingAggregat.oppdatere(hentAggregatHvisEksisterer(behandlingId))
            .medAvklarteDatoer(avklarteUttakDatoer)
            .build();
        lagre(behandlingId, ny);
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<Long> hentIdPåAktivYtelsesFordeling(Long behandlingId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void tilbakestillFordeling(Long behandlingId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void lagre(Long behandlingId, YtelseFordelingAggregat aggregat) {
        ytelseFordelingAggregatMap.put(behandlingId, aggregat);
    }

    @Override
    public DiffResult diffResultat(Long grunnlagId1, Long grunnlagId2, boolean onlyCheckTrackedFields) {
        throw new IkkeImplementertForTestException();
    }
}
