package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
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
                                          UtvidetBehandlingDto dto,
                                          boolean erBehandlingMedGjeldendeVedtak,
                                          LocalDate vedtaksDato) {
        setStandardfelterMedGjeldendeVedtak(behandling, behandlingsresultat, dto, erBehandlingMedGjeldendeVedtak, vedtaksDato);
              dto.setAnsvarligBeslutter(behandling.getAnsvarligBeslutter());
        dto.setBehandlingHenlagt(getBehandlingsResultatType(behandlingsresultat).erHenlagt());
    }

    private static Optional<String> getFristDatoBehandlingPåVent(Behandling behandling) {
        var frist = behandling.getFristDatoBehandlingPåVent();
        if (frist != null) {
            return Optional.of(frist.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return Optional.empty();
    }

    private static Optional<String> getVenteÅrsak(Behandling behandling) {
        var venteårsak = behandling.getVenteårsak();
        if (venteårsak != null) {
            return Optional.of(venteårsak.getKode());
        }
        return Optional.empty();
    }

    private static List<BehandlingÅrsakDto> lagBehandlingÅrsakDto(Behandling behandling) {
        if (!behandling.getBehandlingÅrsaker().isEmpty()) {
            return behandling.getBehandlingÅrsaker().stream().map(BehandlingDtoUtil::map).toList();
        }
        return Collections.emptyList();
    }

    static void setStandardfelter(Behandling behandling,
                                  Behandlingsresultat behandlingsresultat,
                                  BehandlingDto dto,
                                  LocalDate vedtaksDato) {
        dto.setFagsakId(behandling.getFagsakId());
        dto.setId(behandling.getId());
        dto.setUuid(behandling.getUuid());
        dto.setVersjon(behandling.getVersjon());
        dto.setType(behandling.getType());
        dto.setOpprettet(behandling.getOpprettetDato());
        dto.setEndret(behandling.getEndretTidspunkt());
        dto.setEndretAvBrukernavn(behandling.getEndretAv());
        dto.setAvsluttet(behandling.getAvsluttetDato());
        dto.setStatus(behandling.getStatus());
        dto.setBehandlendeEnhetId(behandling.getBehandlendeOrganisasjonsEnhet().enhetId());
        dto.setBehandlendeEnhetNavn(behandling.getBehandlendeOrganisasjonsEnhet().enhetNavn());
        dto.setFørsteÅrsak(førsteÅrsak(behandling).orElse(null));
        dto.setBehandlingsfristTid(behandling.getBehandlingstidFrist());
        dto.setErAktivPapirsøknad(erAktivPapirsøknad(behandling));
        dto.setBehandlingPåVent(behandling.isBehandlingPåVent());
        dto.setBehandlingHenlagt(getBehandlingsResultatType(behandlingsresultat).erHenlagt());
        getFristDatoBehandlingPåVent(behandling).ifPresent(dto::setFristBehandlingPåVent);
        getVenteÅrsak(behandling).ifPresent(dto::setVenteÅrsakKode);
        dto.setOriginalVedtaksDato(vedtaksDato);
        dto.setBehandlingKøet(behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
        dto.setAnsvarligSaksbehandler(behandling.getAnsvarligSaksbehandler());
        dto.setToTrinnsBehandling(behandling.isToTrinnsBehandling());
        dto.setBehandlingÅrsaker(lagBehandlingÅrsakDto(behandling));
        dto.setVilkår(!erAktivPapirsøknad(behandling) ? VilkårDtoMapper.lagVilkarDto(behandling, behandlingsresultat) : List.of());
    }

    static void setStandardfelterMedGjeldendeVedtak(Behandling behandling,
                                                    Behandlingsresultat behandlingsresultat,
                                                    BehandlingDto dto,
                                                    boolean erBehandlingMedGjeldendeVedtak,
                                                    LocalDate vedtaksDato) {
        setStandardfelter(behandling, behandlingsresultat, dto, vedtaksDato);
        dto.setGjeldendeVedtak(erBehandlingMedGjeldendeVedtak);
    }

    private static Optional<BehandlingÅrsakDto> førsteÅrsak(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .sorted(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt))
            .map(BehandlingDtoUtil::map)
            .findFirst();
    }

    private static boolean erAktivPapirsøknad(Behandling behandling) {
        var kriterier = Arrays.asList(
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD,
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER,
            AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER,
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER);
        return !behandling.getÅpneAksjonspunkter(kriterier).isEmpty();
    }

    private static BehandlingÅrsakDto map(BehandlingÅrsak årsak) {
        var dto = new BehandlingÅrsakDto();
        dto.setBehandlingArsakType(årsak.getBehandlingÅrsakType());
        dto.setManueltOpprettet(årsak.erManueltOpprettet());
        return dto;
    }

    private static BehandlingResultatType getBehandlingsResultatType(Behandlingsresultat behandlingsresultat) {
        return Optional.ofNullable(behandlingsresultat).map(Behandlingsresultat::getBehandlingResultatType).orElse(BehandlingResultatType.IKKE_FASTSATT);
    }
}
