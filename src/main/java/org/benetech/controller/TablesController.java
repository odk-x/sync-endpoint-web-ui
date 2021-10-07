package org.benetech.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.benetech.client.OdkClient;
import org.benetech.client.OdkClientFactory;
import org.benetech.model.display.OdkTablesFileManifestEntryDisplay;
import org.benetech.model.rows.RowSelect;
import org.benetech.model.rows.RowSelectList;
import org.benetech.util.RowUtils;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowList;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcome;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcomeList;
import org.opendatakit.aggregate.odktables.rest.entity.RowResource;
import org.opendatakit.aggregate.odktables.rest.entity.RowResourceList;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcome.OutcomeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class TablesController {

  @Autowired
  OdkClientFactory odkClientFactory;

  private static Log logger = LogFactory.getLog(TablesController.class);

  @RequestMapping("/tables/manifest/{tableId}")
  public String tables(@PathVariable("tableId") String tableId, Model model) {

    OdkClient odkClient = odkClientFactory.getOdkClient();
    OdkTablesFileManifest manifest = odkClient.getTableManifest(tableId);

    model.addAttribute("manifest", manifest);
    model.addAttribute("tableId", tableId);
    model.addAttribute("currentPage", "manifest");
    return "odk_tables_manifest";
  }

  @RequestMapping("/tables/attachments/{tableId}")
  public String attachments(@PathVariable("tableId") String tableId, Model model) {
    OdkClient odkClient = odkClientFactory.getOdkClient();

    TableResource tableResource = odkClient.getTableResource(tableId);
    final String schemaETag = tableResource.getSchemaETag();

    List<OdkTablesFileManifestEntryDisplay> mergedManifestDisplay = odkClient
            .getRowResourceList(tableId, tableResource.getSchemaETag(), "", false)
            .getRows()
            .stream()
            .map(Row::getRowId)
            .parallel()
            .map(id -> odkClient.getSingleRowAttachments(tableId, schemaETag, id))
            .flatMap(manifest -> manifest.getFiles().stream())
            .map(OdkTablesFileManifestEntryDisplay::new)
            .collect(Collectors.toList());

    model.addAttribute("manifest", mergedManifestDisplay);
    model.addAttribute("tableId", tableId);
    model.addAttribute("currentPage", "attachments");
    return "odk_tables_attachments";
  }
  
  @GetMapping("/tables/export/{tableId}")
  public String exportForm(@PathVariable("tableId") String tableId, Model model) {
    model.addAttribute("tableId", tableId);
    model.addAttribute("currentPage", "export");
    return "odk_tables_export";
  }

  @RequestMapping("/tables/{tableId}/rows")
  public String rows(@PathVariable("tableId") String tableId,
      @RequestParam(name = "sortColumn", defaultValue = "_savepoint_timestamp",
          required = false) String sortColumn,
      @RequestParam(name = "ascending", defaultValue = "false", required = false) boolean ascending,
      Model model) {

    OdkClient odkClient = odkClientFactory.getOdkClient();

    populateDefaultModel(tableId, sortColumn, ascending, model);
    return "odk_tables_rows";
  }

  @PostMapping("/tables/{tableId}/rows/delete")
  public String deleteRow(
      @PathVariable("tableId") String tableId,
      @ModelAttribute RowSelectList rowSelectList,
      BindingResult bindingResult,
      @RequestParam(name = "sortColumn", defaultValue = "_savepoint_timestamp",
          required = false) String sortColumn,
      @RequestParam(name = "ascending", defaultValue = "false", required = false) boolean ascending,
      Model model) {
    OdkClient odkClient = odkClientFactory.getOdkClient();
    TableResource tableResource = odkClient.getTableResource(tableId);
  
    if (!bindingResult.hasErrors()) {
      RowList putList = new RowList();
      for (RowSelect rowSelect : rowSelectList.getRows()) {
        Row r =  rowSelect.getRow();
        RowResource rowResource = odkClient.getSingleRow(tableId, tableResource.getSchemaETag(), r.getRowId());
        Row row =  RowUtils.resourceToRow(rowResource);
        row.setDeleted(rowSelect.getSelected());
        putList.getRows().add(row);
      }
      putList.setDataETag(tableResource.getDataETag());
      RowOutcomeList rowOutcomeList= odkClient.putRowList(tableId, tableResource.getSchemaETag(), putList);
      int rowsNotDeleted = 0;
      for (RowOutcome outcome : rowOutcomeList.getRows()) {
        rowsNotDeleted +=  (outcome.getOutcome() != OutcomeType.SUCCESS ? 1 : 0);
      }
      model.addAttribute("msg",
      "Rows deleted. Issues deleting " + rowsNotDeleted + " rows");
    } else {
      model.addAttribute("msg", bindingResult.getAllErrors().toString());
    }

    populateDefaultModel(tableId, sortColumn, ascending, model);
    model.addAttribute("css", "info");
    return "odk_tables_rows";
  }



  private void populateDefaultModel(String tableId, String sortColumn, boolean ascending,
      Model model) {
    OdkClient odkClient = odkClientFactory.getOdkClient();

    TableResource tableResource = odkClient.getTableResource(tableId);
    RowResourceList rowResourceList =
        odkClient.getRowResourceList(tableId, tableResource.getSchemaETag(), sortColumn, ascending);
    RowSelectList rowSelectList = new RowSelectList();
    for (RowResource rowResource : rowResourceList.getRows()) {
      RowSelect rs = new RowSelect();
      rs.setRow(rowResource);
      rowSelectList.addRow(rs);
    }
    model.addAttribute("tableResource", tableResource);
    model.addAttribute("rowSelectList", rowSelectList);
    model.addAttribute("tableId", tableId);
    model.addAttribute("ascending", ascending);
    model.addAttribute("sortColumn", sortColumn);
    model.addAttribute("currentPage", "row");
  }

  @GetMapping("/tables/upload")
  public String uploadForm(Model model) {
    OdkClient odkClient = odkClientFactory.getOdkClient();
    return "odk_tables_upload";
  }
}
