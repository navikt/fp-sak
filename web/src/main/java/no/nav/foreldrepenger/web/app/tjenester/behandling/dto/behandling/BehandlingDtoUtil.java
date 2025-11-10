package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårDtoMapper;

final class BehandlingDtoUtil {

    private BehandlingDtoUtil() {
    }

    static void settStandardfelterUtvidet(Behandling behandling,
                                          Behandlingsresultat behandlingsresultat,
                                          UtvidetBehandlingDto dto) {
        setStandardfelter(behandling, behandlingsresultat, dto);
        dto.setBehandlingHenlagt(getBehandlingsResultatType(behandlingsresultat).erHenlagt());
    }

    static Optional<String> getFristDatoBehandlingPåVent(Behandling behandling) {
        var frist = behandling.getFristDatoBehandlingPåVent();
        if (frist != null) {
            return Optional.of(frist.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return Optional.empty();
    }

    static List<BehandlingÅrsakDto> lagBehandlingÅrsakDto(Behandling behandling) {
        if (!behandling.getBehandlingÅrsaker().isEmpty()) {
            return behandling.getBehandlingÅrsaker().stream().map(BehandlingDtoUtil::map).toList();
        }
        return Collections.emptyList();
    }

    static void setStandardfelter(Behandling behandling,
                                  Behandlingsresultat behandlingsresultat,
                                  UtvidetBehandlingDto dto) {
        dto.setUuid(behandling.getUuid());
        dto.setId(behandling.getId());
        dto.setOpprettet(behandling.getOpprettetTidspunkt());
        dto.setVersjon(behandling.getVersjon());
        dto.setType(behandling.getType());
        dto.setStatus(behandling.getStatus());
        dto.setBehandlendeEnhetId(behandling.getBehandlendeOrganisasjonsEnhet().enhetId());
        dto.setBehandlendeEnhetNavn(behandling.getBehandlendeOrganisasjonsEnhet().enhetNavn());
        dto.setAktivPapirsøknad(erAktivPapirsøknad(behandling));
        dto.setBehandlingPåVent(behandling.isBehandlingPåVent());
        dto.setBehandlingHenlagt(getBehandlingsResultatType(behandlingsresultat).erHenlagt());
        getFristDatoBehandlingPåVent(behandling).ifPresent(dto::setFristBehandlingPåVent);
        dto.setBehandlingÅrsaker(lagBehandlingÅrsakDto(behandling));
        dto.setVilkår(!erAktivPapirsøknad(behandling) ? VilkårDtoMapper.lagVilkarDto(behandling, behandlingsresultat) : List.of());
    }

    static boolean erAktivPapirsøknad(Behandling behandling) {
        var kriterier = Arrays.asList(
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD,
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER,
            AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER,
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER);
        return !behandling.getÅpneAksjonspunkter(kriterier).isEmpty();
    }

    static BehandlingÅrsakDto map(BehandlingÅrsak årsak) {
        var dto = new BehandlingÅrsakDto();
        dto.setBehandlingArsakType(årsak.getBehandlingÅrsakType());
        dto.setManueltOpprettet(årsak.erManueltOpprettet());
        return dto;
    }

    static BehandlingResultatType getBehandlingsResultatType(Behandlingsresultat behandlingsresultat) {
        return Optional.ofNullable(behandlingsresultat).map(Behandlingsresultat::getBehandlingResultatType).orElse(BehandlingResultatType.IKKE_FASTSATT);
    }
}
