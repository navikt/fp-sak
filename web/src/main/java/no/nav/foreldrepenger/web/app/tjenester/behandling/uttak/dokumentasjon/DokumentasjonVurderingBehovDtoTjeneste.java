package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

@ApplicationScoped
public class DokumentasjonVurderingBehovDtoTjeneste {

    private BehandlingRepository behandlingRepository;
    private UttakInputTjeneste uttakInputTjeneste;
    private VurderUttakDokumentasjonAksjonspunktUtleder utleder;
    private AktivitetskravArbeidRepository aktivitetskravArbeidRepository;

    @Inject
    public DokumentasjonVurderingBehovDtoTjeneste(BehandlingRepository behandlingRepository,
                                                  UttakInputTjeneste uttakInputTjeneste,
                                                  VurderUttakDokumentasjonAksjonspunktUtleder utleder,
                                                  AktivitetskravArbeidRepository aktivitetskravArbeidRepository) {
        this.behandlingRepository = behandlingRepository;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.utleder = utleder;
        this.aktivitetskravArbeidRepository = aktivitetskravArbeidRepository;
    }

    DokumentasjonVurderingBehovDtoTjeneste() {
        //CDI
    }

    public List<DokumentasjonVurderingBehovDto> lagDtos(UuidDto behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId.getBehandlingUuid());
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        var behov = utleder.utledDokumentasjonVurderingBehov(uttakInput);
        var aktivitetskravGrunnlagEntitet = aktivitetskravArbeidRepository.hentGrunnlag(behandling.getId());

        return behov.stream().map(b -> {
            Set<AktivitetskravArbeidPeriodeEntitet> aktivitetskravPerioder =
                b.behov().årsak() == DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID
                    && aktivitetskravGrunnlagEntitet.isPresent() ? finnAktivitetskravArbeidPerioder(b.oppgittPeriode().getTidsperiode(),
                    aktivitetskravGrunnlagEntitet.get()) : Set.of();
            return DokumentasjonVurderingBehovDto.from(b, aktivitetskravPerioder);
        }).toList();
    }

    private Set<AktivitetskravArbeidPeriodeEntitet> finnAktivitetskravArbeidPerioder(DatoIntervallEntitet tidsperiode,
                                                                                     AktivitetskravGrunnlagEntitet ag) {
        return ag.getAktivitetskravPerioderMedArbeidEntitet()
            .map(perioder -> perioder.getAktivitetskravArbeidPeriodeListe()
                .stream()
                .filter(per -> per.getPeriode().overlapper(tidsperiode))
                .collect(Collectors.toSet()))
            .orElse(Set.of());
    }
}
