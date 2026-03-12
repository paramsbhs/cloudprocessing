import { useState, useRef } from "react";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle,
} from "./ui/dialog";
import { Button } from "./ui/button";
import { Label } from "./ui/label";
import { Upload, FileText, Loader2 } from "lucide-react";
import { api, uploadToS3, FileRecord } from "../api";

interface UploadModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUploadComplete: (file: FileRecord) => void;
}

type Step = "idle" | "creating" | "uploading" | "confirming" | "done" | "error";

const stepLabel: Record<Step, string> = {
  idle:       "Start Upload",
  creating:   "Creating record…",
  uploading:  "Uploading to S3…",
  confirming: "Confirming…",
  done:       "Done!",
  error:      "Retry",
};

function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${Math.round((bytes / Math.pow(k, i)) * 10) / 10} ${sizes[i]}`;
}

function detectContentType(file: File): string {
  if (file.type) return file.type;
  const ext = file.name.split(".").pop()?.toLowerCase();
  const map: Record<string, string> = {
    pdf: "application/pdf",
    zip: "application/zip",
    gz:  "application/gzip",
    jpg: "image/jpeg", jpeg: "image/jpeg",
    png: "image/png",
    gif: "image/gif",
    txt: "text/plain",
  };
  return map[ext ?? ""] ?? "application/octet-stream";
}

export function UploadModal({ open, onOpenChange, onUploadComplete }: UploadModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [step, setStep] = useState<Step>("idle");
  const [error, setError] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = (e: React.DragEvent) => { e.preventDefault(); setIsDragging(true); };
  const handleDragLeave = () => setIsDragging(false);
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files[0]) setSelectedFile(e.dataTransfer.files[0]);
  };
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files?.[0]) setSelectedFile(e.target.files[0]);
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    setError("");

    const contentType = detectContentType(selectedFile);

    try {
      // Step 1 — create file record, get presigned upload URL
      setStep("creating");
      const record = await api.files.create({
        originalName: selectedFile.name,
        contentType,
        sizeBytes: selectedFile.size,
      });

      // Step 2 — PUT file bytes directly to S3
      setStep("uploading");
      await uploadToS3(record.uploadUrl!, selectedFile);

      // Step 3 — confirm with the backend (verifies via HeadObject)
      setStep("confirming");
      const confirmed = await api.files.confirmUpload(record.id);

      setStep("done");
      setTimeout(() => {
        onUploadComplete(confirmed);
        reset();
      }, 600);
    } catch (err: any) {
      setStep("error");
      setError(err.message ?? "Upload failed");
    }
  };

  const reset = () => {
    setSelectedFile(null);
    setStep("idle");
    setError("");
    onOpenChange(false);
  };

  const busy = step === "creating" || step === "uploading" || step === "confirming";

  return (
    <Dialog open={open} onOpenChange={busy ? undefined : reset}>
      <DialogContent className="bg-card border-border sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Upload File</DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Drop zone */}
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => !busy && fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors
              ${busy ? "opacity-50 cursor-not-allowed" : "cursor-pointer"}
              ${isDragging
                ? "border-primary bg-primary/5"
                : "border-border hover:border-primary/50 hover:bg-primary/5"}`}
          >
            <Upload className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
            <p className="text-sm text-foreground mb-1">
              Drop your file here or <span className="text-primary">browse</span>
            </p>
            <p className="text-xs text-muted-foreground">
              PDF, image, zip, or text · max 100 MB
            </p>
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              onChange={handleFileSelect}
              disabled={busy}
            />
          </div>

          {/* File preview */}
          {selectedFile && (
            <div className="space-y-3 border border-border rounded-lg p-4 bg-secondary/30">
              <div className="flex items-start gap-3">
                <FileText className="w-5 h-5 text-muted-foreground mt-0.5 shrink-0" />
                <div className="space-y-2 flex-1 min-w-0">
                  <div>
                    <Label className="text-xs text-muted-foreground">File Name</Label>
                    <p className="text-sm font-medium truncate">{selectedFile.name}</p>
                  </div>
                  <div>
                    <Label className="text-xs text-muted-foreground">Type</Label>
                    <p className="text-sm text-muted-foreground">
                      {detectContentType(selectedFile)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-xs text-muted-foreground">Size</Label>
                    <p className="text-sm text-muted-foreground">
                      {formatBytes(selectedFile.size)}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {error && <p className="text-sm text-red-400">{error}</p>}

          <Button
            onClick={handleUpload}
            disabled={!selectedFile || busy}
            className="w-full bg-primary hover:bg-primary/90 disabled:opacity-50"
          >
            {busy && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
            {stepLabel[step]}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
