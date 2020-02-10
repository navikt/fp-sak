package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.uttak.OrgManuellÅrsakEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;

class UttakRepositoryStub extends UttakRepository {

    private Map<Long, UttakResultatEntitet> map = new HashMap<>();

    @Override
    public void lagreOpprinneligUttakResultatPerioder(Long behandlingId, UttakResultatPerioderEntitet perioder) {
        map.put(behandlingId, new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(perioder).build());
    }

    @Override
    public void lagreOverstyrtUttakResultatPerioder(Long behandlingId, UttakResultatPerioderEntitet overstyrtPerioder) {
        UttakResultatEntitet uttakResultat = map.get(behandlingId);
        uttakResultat.setOverstyrtPerioder(overstyrtPerioder);
    }

    @Override
    public Optional<UttakResultatEntitet> hentUttakResultatHvisEksisterer(Long behandlingId) {
        if (map.containsKey(behandlingId)) {
            return Optional.of(map.get(behandlingId));
        }
        return Optional.empty();
    }

    @Override
    public UttakResultatEntitet hentUttakResultat(Long behandlingId) {
        return map.get(behandlingId);
    }

    @Override
    public Optional<UttakResultatEntitet> hentUttakResultatPåId(Long id) {
        return map.values().stream().filter(ur -> Objects.equals(ur.getId(), id)).findFirst();
    }

    @Override
    public void lagreUttaksperiodegrense(Long behandlingId, Uttaksperiodegrense uttaksperiodegrense) {
        throw new IllegalStateException("Ikke implementert");
    }

    @Override
    public void ryddUttaksperiodegrense(Long behandlingId) {
        throw new IllegalStateException("Ikke implementert");
    }

    @Override
    public Uttaksperiodegrense hentUttaksperiodegrense(Long behandlingId) {
        throw new IllegalStateException("Ikke implementert");
    }

    @Override
    public Optional<Uttaksperiodegrense> hentUttaksperiodegrenseHvisEksisterer(Long behandlingId) {
        throw new IllegalStateException("Ikke implementert");
    }

    @Override
    public EndringsresultatSnapshot finnAktivAggregatId(Long behandlingId) {
        throw new IllegalStateException("Ikke implementert");    }

    @Override
    public EndringsresultatSnapshot finnAktivUttakPeriodeGrenseAggregatId(Long behandlingId) {
        throw new IllegalStateException("Ikke implementert");    }

    @Override
    public List<OrgManuellÅrsakEntitet> finnOrgManuellÅrsak(String virksomhetsnummer) {
        throw new IllegalStateException("Ikke implementert");
    }

    @Override
    public void deaktivterAktivtResultat(Long behandlingId) {
        throw new IllegalStateException("Ikke implementert");
    }
}
