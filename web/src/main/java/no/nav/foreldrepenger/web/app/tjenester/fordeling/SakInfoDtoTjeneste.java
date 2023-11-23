package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kontrakter.fordel.SakInfoV2Dto;
import no.nav.foreldrepenger.kontrakter.fordel.SaksnummerDto;
import no.nav.foreldrepenger.kontrakter.fordel.YtelseTypeDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class SakInfoDtoTjeneste {
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public SakInfoDtoTjeneste(BehandlingRepository behandlingRepository,
                              FamilieHendelseRepository familieHendelseRepository,
                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }
    public SakInfoDtoTjeneste() {
        //CDI
    }

    /**
     * @deprecated fjernes etter bytte til mapSakInfoV2Dto
     */
    @Deprecated(forRemoval = true)
    public SakInfoDto mapSakInfoDto(Fagsak fagsak) {
        var sisteYtelsesBehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId()).orElse(null);
        if (sisteYtelsesBehandling != null) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(sisteYtelsesBehandling.getId());
        LocalDate førsteUttaksdato;
        try {
            førsteUttaksdato = stp.getFørsteUttaksdato();
        } catch (Exception e) {
            førsteUttaksdato = stp.getSkjæringstidspunktHvisUtledet().orElse(null);
        }
        var familiehendelseInfoDto = familieHendelseRepository.hentAggregatHvisEksisterer(sisteYtelsesBehandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(this::mapFamiliehendelseInfoDto);

            return mapTilSakInfoDto(fagsak, familiehendelseInfoDto.orElse(null), førsteUttaksdato);
        } else {
            return mapTilSakInfoDto(fagsak, null, null);
        }
    }

    public SakInfoV2Dto mapSakInfoV2Dto(Fagsak fagsak) {
        var sisteYtelsesBehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId()).orElse(null);
        if (sisteYtelsesBehandling != null) {
            var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(sisteYtelsesBehandling.getId());
            LocalDate førsteUttaksdato;
            try {
                førsteUttaksdato = stp.getFørsteUttaksdato();
            } catch (Exception e) {
                førsteUttaksdato = stp.getSkjæringstidspunktHvisUtledet().orElse(null);
            }
            var familiehendelseInfoDto = familieHendelseRepository.hentAggregatHvisEksisterer(sisteYtelsesBehandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(this::mapFamiliehendelseInfoV2Dto);

            return mapTilSakInfoV2Dto(fagsak, familiehendelseInfoDto.orElse(null), førsteUttaksdato);
        } else {
            return mapTilSakInfoV2Dto(fagsak, null, null);
        }
    }

    /**
     * @deprecated fjernes etter bytte til mapSakInfoV2Dto
     */
    @Deprecated(forRemoval = true)
    private SakInfoDto.FamiliehendelseInfoDto mapFamiliehendelseInfoDto(FamilieHendelseEntitet familiehendelse) {
        return new SakInfoDto.FamiliehendelseInfoDto(familiehendelse.getSkjæringstidspunkt(), mapFamilieHendelseTypeDto(familiehendelse.getType()));
    }

    /**
     * @deprecated fjernes etter bytte til mapSakInfoV2Dto
     */
    @Deprecated(forRemoval = true)
    private SakInfoDto mapTilSakInfoDto(Fagsak fagsak, SakInfoDto.FamiliehendelseInfoDto familiehendelseInfoDto, LocalDate førsteUttaksdato) {
        return new SakInfoDto(new SaksnummerDto(fagsak.getSaksnummer().getVerdi()), mapFagsakYtelseTypeDto(fagsak.getYtelseType()), fagsak.getOpprettetTidspunkt().toLocalDate(),
            mapFagsakStatusDto(fagsak.getStatus()), familiehendelseInfoDto, førsteUttaksdato);
    }

    /**
     * @deprecated fjernes etter bytte til mapSakInfoV2Dto
     */
    @Deprecated(forRemoval = true)
    public SakInfoDto.FamilieHendelseTypeDto mapFamilieHendelseTypeDto(FamilieHendelseType familieHendelseType) {
        return switch (familieHendelseType) {
            case FØDSEL -> SakInfoDto.FamilieHendelseTypeDto.FØDSEL;
            case TERMIN -> SakInfoDto.FamilieHendelseTypeDto.TERMIN;
            case ADOPSJON -> SakInfoDto.FamilieHendelseTypeDto.ADOPSJON;
            case OMSORG -> SakInfoDto.FamilieHendelseTypeDto.OMSORG;
            case UDEFINERT -> null;
        };
    }

    /**
     * @deprecated fjernes etter bytte til mapSakInfoV2Dto
     */
    @Deprecated(forRemoval = true)
    public SakInfoDto.FagsakYtelseTypeDto mapFagsakYtelseTypeDto(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> SakInfoDto.FagsakYtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> SakInfoDto.FagsakYtelseTypeDto.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD -> SakInfoDto.FagsakYtelseTypeDto.ENGANGSTØNAD;
            case UDEFINERT -> throw new IllegalStateException("Mangler fagsakYtelseType");
        };
    }

    /**
     * @deprecated fjernes etter bytte til mapSakInfoV2Dto
     */
    @Deprecated(forRemoval = true)
    public SakInfoDto.FagsakStatusDto mapFagsakStatusDto(FagsakStatus fagsakStatus) {
        return switch (fagsakStatus) {
            case OPPRETTET, UNDER_BEHANDLING -> SakInfoDto.FagsakStatusDto.UNDER_BEHANDLING;
            case LØPENDE -> SakInfoDto.FagsakStatusDto.LØPENDE;
            case AVSLUTTET -> SakInfoDto.FagsakStatusDto.AVSLUTTET;
        };
    }

    private SakInfoV2Dto mapTilSakInfoV2Dto(Fagsak fagsak, SakInfoV2Dto.FamiliehendelseInfoDto familiehendelseInfoDto, LocalDate førsteUttaksdato) {
        return new SakInfoV2Dto(new SaksnummerDto(fagsak.getSaksnummer().getVerdi()),
            mapYtelseTypeV2Dto(fagsak.getYtelseType()),
            mapFagsakStatusV2Dto(fagsak.getStatus()),
            familiehendelseInfoDto,
            fagsak.getOpprettetTidspunkt().toLocalDate(),
            førsteUttaksdato);
    }

    public YtelseTypeDto mapYtelseTypeV2Dto(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD -> YtelseTypeDto.ENGANGSTØNAD;
            case UDEFINERT -> throw new IllegalStateException("Mangler fagsakYtelseType");
        };
    }

    public SakInfoV2Dto.FagsakStatusDto mapFagsakStatusV2Dto(FagsakStatus fagsakStatus) {
        return switch (fagsakStatus) {
            case OPPRETTET, UNDER_BEHANDLING -> SakInfoV2Dto.FagsakStatusDto.UNDER_BEHANDLING;
            case LØPENDE -> SakInfoV2Dto.FagsakStatusDto.LØPENDE;
            case AVSLUTTET -> SakInfoV2Dto.FagsakStatusDto.AVSLUTTET;
        };
    }

    private SakInfoV2Dto.FamiliehendelseInfoDto mapFamiliehendelseInfoV2Dto(FamilieHendelseEntitet familiehendelse) {
        return new SakInfoV2Dto.FamiliehendelseInfoDto(familiehendelse.getSkjæringstidspunkt(), mapFamilieHendelseTypeV2Dto(familiehendelse.getType()));
    }

    public SakInfoV2Dto.FamilieHendelseTypeDto mapFamilieHendelseTypeV2Dto(FamilieHendelseType familieHendelseType) {
        return switch (familieHendelseType) {
            case FØDSEL -> SakInfoV2Dto.FamilieHendelseTypeDto.FØDSEL;
            case TERMIN -> SakInfoV2Dto.FamilieHendelseTypeDto.TERMIN;
            case ADOPSJON -> SakInfoV2Dto.FamilieHendelseTypeDto.ADOPSJON;
            case OMSORG -> SakInfoV2Dto.FamilieHendelseTypeDto.OMSORG;
            case UDEFINERT -> null; // bør ikke være mulig - det ligger ca 335 inaktive hendelser fra 2018 (antageligvis en feil)
        };
    }
}
