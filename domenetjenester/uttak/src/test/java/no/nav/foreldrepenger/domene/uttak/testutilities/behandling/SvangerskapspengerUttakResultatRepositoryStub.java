package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class SvangerskapspengerUttakResultatRepositoryStub extends SvangerskapspengerUttakResultatRepository {

    private final Map<Long, SvangerskapspengerUttakResultatEntitet> map = new ConcurrentHashMap<>();

    @Override
    public void lagre(Long behandlingId,
                      SvangerskapspengerUttakResultatEntitet svangerskapspengerUttakResultatEntitet) {
        map.put(behandlingId, svangerskapspengerUttakResultatEntitet);
    }

    @Override
    public Optional<SvangerskapspengerUttakResultatEntitet> hentHvisEksisterer(Long behandlingId) {
        return Optional.of(map.get(behandlingId));
    }
}
