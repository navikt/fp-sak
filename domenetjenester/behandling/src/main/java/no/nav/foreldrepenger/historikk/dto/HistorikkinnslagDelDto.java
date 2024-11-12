package no.nav.foreldrepenger.historikk.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

public class HistorikkinnslagDelDto {

    private String begrunnelsetekst;
    private String begrunnelseFritekst;
    private HistorikkinnslagHendelseDto hendelse;
    private List<HistorikkinnslagOpplysningDto> opplysninger;
    private HistorikkinnslagSoeknadsperiodeDto soeknadsperiode;
    private SkjermlenkeType skjermlenke;
    private String årsaktekst;
    private HistorikkInnslagTemaDto tema;
    private HistorikkInnslagGjeldendeFraDto gjeldendeFra;
    private String resultat;
    private List<HistorikkinnslagEndretFeltDto> endredeFelter;
    private List<HistorikkinnslagTotrinnsVurderingDto> aksjonspunkter;

    static List<HistorikkinnslagDelDto> mapFra(List<HistorikkinnslagDel> historikkinnslagDelList) {
        List<HistorikkinnslagDelDto> historikkinnslagDelDtoList = new ArrayList<>();
        for (var historikkinnslagDel : historikkinnslagDelList) {
            historikkinnslagDelDtoList.add(mapFra(historikkinnslagDel));
        }
        return historikkinnslagDelDtoList;
    }

    private static HistorikkinnslagDelDto mapFra(HistorikkinnslagDel historikkinnslagDel) {
        var dto = new HistorikkinnslagDelDto();
        var begrunnelseKodeverdi = historikkinnslagDel.getBegrunnelseFelt().flatMap(HistorikkinnslagDelDto::finnÅrsakKodeListe);
        if (begrunnelseKodeverdi.isEmpty()) {
            historikkinnslagDel.getBegrunnelse().ifPresent(dto::setBegrunnelseFritekst);
        } else {
            dto.setBegrunnelsetekst(begrunnelseKodeverdi.get().getNavn());
        }
        historikkinnslagDel.getAarsakFelt().flatMap(HistorikkinnslagDelDto::finnÅrsakKodeListe).map(Kodeverdi::getNavn).ifPresent(dto::setÅrsaktekst);
        historikkinnslagDel.getTema().ifPresent(felt -> dto.setTema(HistorikkInnslagTemaDto.mapFra(felt)));
        historikkinnslagDel.getGjeldendeFraFelt().ifPresent(felt -> {
            if (felt.getNavn() != null && felt.getNavnVerdi() != null && felt.getTilVerdi() != null) {
                dto.setGjeldendeFra(felt.getTilVerdi(), felt.getNavn(), felt.getNavnVerdi());
            } else if (felt.getTilVerdi() != null) {
                dto.setGjeldendeFra(felt.getTilVerdi());
            }
        });
        historikkinnslagDel.getResultat().ifPresent(dto::setResultat);
        historikkinnslagDel.getHendelse().ifPresent(hendelse -> {
            var hendelseDto = HistorikkinnslagHendelseDto.mapFra(hendelse);
            dto.setHendelse(hendelseDto);
        });
        historikkinnslagDel.getSkjermlenke().ifPresent(skjermlenke -> {
            var type = SkjermlenkeType.fraKode(skjermlenke);
            dto.setSkjermlenke(type);
        });
        if (!historikkinnslagDel.getTotrinnsvurderinger().isEmpty()) {
            dto.setAksjonspunkter(HistorikkinnslagTotrinnsVurderingDto.mapFra(historikkinnslagDel.getTotrinnsvurderinger()));
        }
        if (!historikkinnslagDel.getOpplysninger().isEmpty()) {
            dto.setOpplysninger(HistorikkinnslagOpplysningDto.mapFra(historikkinnslagDel.getOpplysninger()));
        }
        if (!historikkinnslagDel.getEndredeFelt().isEmpty()) {
            dto.setEndredeFelter(HistorikkinnslagEndretFeltDto.mapFra(historikkinnslagDel.getEndredeFelt()));
        }
        historikkinnslagDel.getAvklartSoeknadsperiode().ifPresent(soeknadsperiode -> {
            var soeknadsperiodeDto = HistorikkinnslagSoeknadsperiodeDto.mapFra(soeknadsperiode);
            dto.setSoeknadsperiode(soeknadsperiodeDto);
        });
        return dto;
    }

    public static Optional<Kodeverdi> finnÅrsakKodeListe(HistorikkinnslagFelt aarsak) {

        var aarsakVerdi = aarsak.getTilVerdi();
        if (Objects.equals("-", aarsakVerdi)) {
            return Optional.empty();
        }
        if (aarsak.getKlTilVerdi() == null) {
            return Optional.empty();
        }

        var kodeverdiMap = HistorikkInnslagTekstBuilder.KODEVERK_KODEVERDI_MAP.get(aarsak.getKlTilVerdi());
        if (kodeverdiMap == null) {
            throw new IllegalStateException("Har ikke støtte for HistorikkinnslagFelt#klTilVerdi=" + aarsak.getKlTilVerdi());
        }
        return Optional.ofNullable(kodeverdiMap.get(aarsakVerdi));
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public HistorikkinnslagHendelseDto getHendelse() {
        return hendelse;
    }

    public void setHendelse(HistorikkinnslagHendelseDto hendelse) {
        this.hendelse = hendelse;
    }

    public SkjermlenkeType getSkjermlenke() {
        return skjermlenke;
    }

    public void setSkjermlenke(SkjermlenkeType skjermlenke) {
        this.skjermlenke = skjermlenke;
    }

    public HistorikkInnslagTemaDto getTema() {
        return tema;
    }

    public void setTema(HistorikkInnslagTemaDto tema) {
        this.tema = tema;
    }

    public HistorikkInnslagGjeldendeFraDto getGjeldendeFra() {
        return gjeldendeFra;
    }

    public void setGjeldendeFra(String fra) {
        if (this.gjeldendeFra == null) {
            this.gjeldendeFra = new HistorikkInnslagGjeldendeFraDto(fra);
        } else {
            this.gjeldendeFra.setFra(fra);
        }
    }

    public void setGjeldendeFra(String fra, String navn, String verdi) {
        if (this.gjeldendeFra == null) {
            this.gjeldendeFra = new HistorikkInnslagGjeldendeFraDto(fra, navn, verdi);
        } else {
            this.gjeldendeFra.setFra(fra);
            this.gjeldendeFra.setNavn(navn);
            this.gjeldendeFra.setVerdi(verdi);
        }
    }

    public String getResultat() {
        return resultat;
    }

    public void setResultat(String resultat) {
        this.resultat = resultat;
    }

    public List<HistorikkinnslagEndretFeltDto> getEndredeFelter() {
        return endredeFelter;
    }

    public void setEndredeFelter(List<HistorikkinnslagEndretFeltDto> endredeFelter) {
        this.endredeFelter = endredeFelter;
    }

    public List<HistorikkinnslagOpplysningDto> getOpplysninger() {
        return opplysninger;
    }

    public void setOpplysninger(List<HistorikkinnslagOpplysningDto> opplysninger) {
        this.opplysninger = opplysninger;
    }

    public HistorikkinnslagSoeknadsperiodeDto getSoeknadsperiode() {
        return soeknadsperiode;
    }

    public void setSoeknadsperiode(HistorikkinnslagSoeknadsperiodeDto soeknadsperiode) {
        this.soeknadsperiode = soeknadsperiode;
    }

    public List<HistorikkinnslagTotrinnsVurderingDto> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public void setAksjonspunkter(List<HistorikkinnslagTotrinnsVurderingDto> aksjonspunkter) {
        this.aksjonspunkter = aksjonspunkter;
    }

    public String getBegrunnelsetekst() {
        return begrunnelsetekst;
    }

    public void setBegrunnelsetekst(String begrunnelsetekst) {
        this.begrunnelsetekst = begrunnelsetekst;
    }

    public String getÅrsaktekst() {
        return årsaktekst;
    }

    public void setÅrsaktekst(String årsaktekst) {
        this.årsaktekst = årsaktekst;
    }

    public void setGjeldendeFra(HistorikkInnslagGjeldendeFraDto gjeldendeFra) {
        this.gjeldendeFra = gjeldendeFra;
    }
}
